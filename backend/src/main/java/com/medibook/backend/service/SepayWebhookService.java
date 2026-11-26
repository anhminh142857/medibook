package com.medibook.backend.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.PaymentStatus;
import com.medibook.backend.entity.PaymentTransaction;
import com.medibook.backend.entity.SepayWebhookEvent;
import com.medibook.backend.repository.AppointmentRepository;
import com.medibook.backend.repository.PaymentTransactionRepository;
import com.medibook.backend.repository.SepayWebhookEventRepository;

@Service
public class SepayWebhookService {

    private static final Logger log =
            LoggerFactory.getLogger(SepayWebhookService.class);

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter BANK_TIME =
            DateTimeFormatter
                    .ofPattern("uuuu-MM-dd HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final Pattern PAYMENT_CODE = Pattern.compile(
            "(?<![A-Za-z0-9])BAM[0-9]{8}(?![A-Za-z0-9])"
    );

    private final AppointmentRepository appointmentRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final SepayWebhookEventRepository eventRepository;
    private final String expectedAccount;
    private final String expectedGateway;

    public SepayWebhookService(
            AppointmentRepository appointmentRepository,
            PaymentTransactionRepository paymentRepository,
            SepayWebhookEventRepository eventRepository,
            @Value("${app.sepay.receiver-account:}")
            String expectedAccount,
            @Value("${app.sepay.receiver-gateway:}")
            String expectedGateway
    ) {
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.expectedAccount = expectedAccount;
        this.expectedGateway = expectedGateway;
    }

    @Transactional
    public void handle(Map<String, Object> payload) {
        requireReceiverConfigured();

        if (payload == null) {
            throw error(HttpStatus.BAD_REQUEST, "Thieu payload.");
        }

        long sepayId = positiveLong(payload.get("id"), "id");
        long amount = positiveLong(
                payload.get("transferAmount"),
                "transferAmount"
        );

        String transferType = text(payload, "transferType");
        String account = text(payload, "accountNumber");
        String gateway = text(payload, "gateway");
        String code = text(payload, "code");
        String content = text(payload, "content");
        String transactionDate = text(payload, "transactionDate");

        if (!"in".equals(transferType)
                && !"out".equals(transferType)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Loai giao dich khong hop le."
            );
        }

        if (account.isBlank() || gateway.isBlank()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Thieu tai khoan hoac ngan hang."
            );
        }

        LocalDateTime bankTime = parseBankTime(transactionDate);
        LocalDateTime receivedAt = LocalDateTime.now(ZONE);

        String eventKey = "sandbox:" + sepayId;

        String fingerprint = fingerprint(
                List.of(
                        Long.toString(sepayId),
                        Long.toString(amount),
                        transferType,
                        account,
                        gateway,
                        code,
                        content,
                        transactionDate
                )
        );

        if (alreadyProcessed(eventKey, fingerprint)) {
            return;
        }

        if (!expectedAccount.equals(account)
                || !expectedGateway.equals(gateway)) {
            recordEvent(
                    eventKey, fingerprint, "IGNORED", null,
                    "RECEIVER_MISMATCH", receivedAt
            );
            return;
        }

        if (!"in".equals(transferType)) {
            recordEvent(
                    eventKey, fingerprint, "IGNORED", null,
                    "OUTGOING_TRANSFER", receivedAt
            );
            return;
        }

        Set<String> candidates = new LinkedHashSet<>();
        collectCodes(candidates, code);
        collectCodes(candidates, content);

        if (candidates.isEmpty()) {
            recordEvent(
                    eventKey, fingerprint, "UNMATCHED", null,
                    "NO_PAYMENT_CODE", receivedAt
            );
            return;
        }

        if (candidates.size() != 1) {
            recordEvent(
                    eventKey, fingerprint, "UNMATCHED", null,
                    "MULTIPLE_PAYMENT_CODES", receivedAt
            );
            return;
        }

        String txnRef = candidates.iterator().next();

        // Neu SePay co tra code, code phai khop ma trong noi dung.
        if (!code.isBlank() && !code.equals(txnRef)) {
            recordEvent(
                    eventKey, fingerprint, "UNMATCHED", null,
                    "PAYMENT_CODE_CONFLICT", receivedAt
            );
            return;
        }

        Long appointmentId = paymentRepository
                .findAppointmentIdByTxnRef(txnRef)
                .orElse(null);

        if (appointmentId == null) {
            recordEvent(
                    eventKey, fingerprint, "UNMATCHED", null,
                    "PAYMENT_NOT_FOUND", receivedAt
            );
            return;
        }

        // Cung thu tu khoa voi luong tao thanh toan:
        // lich hen truoc, giao dich sau.
        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() -> error(
                        HttpStatus.CONFLICT,
                        "Lich hen cua giao dich khong ton tai."
                ));

        PaymentTransaction payment = paymentRepository
                .findByTxnRefForUpdate(txnRef)
                .orElseThrow(() -> error(
                        HttpStatus.CONFLICT,
                        "Giao dich khong con ton tai."
                ));

        if (alreadyProcessed(eventKey, fingerprint)) {
            return;
        }

        if (!"SEPAY".equals(payment.getProvider())
                || !"VND".equals(payment.getCurrency())) {
            recordEvent(
                    eventKey, fingerprint, "IGNORED", payment.getId(),
                    "PROVIDER_OR_CURRENCY_MISMATCH", receivedAt
            );
            return;
        }

        String reviewReason = null;

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            reviewReason = "ADDITIONAL_TRANSFER_AFTER_SUCCESS";
        } else if (payment.isRequiresReview()) {
            reviewReason = "PAYMENT_ALREADY_REQUIRES_REVIEW";
        } else if (payment.getStatus() != PaymentStatus.PENDING) {
            reviewReason = "PAYMENT_NOT_PENDING";
        } else if (!"PENDING".equals(appointment.getStatus())) {
            reviewReason = "APPOINTMENT_NOT_PENDING";
        } else if (payment.getAmount() == null
                || payment.getAmount().longValue() != amount) {
            reviewReason = "AMOUNT_MISMATCH";
        } else if (bankTime.isAfter(receivedAt.plusMinutes(5))) {
            reviewReason = "TRANSACTION_TIME_IN_FUTURE";
        } else if (bankTime.isBefore(
                payment.getCreatedAt().truncatedTo(ChronoUnit.SECONDS)
        )) {
            reviewReason = "TRANSFER_BEFORE_PAYMENT_CREATED";
        } else if (!bankTime.isBefore(payment.getExpiresAt())
                || !receivedAt.isBefore(payment.getExpiresAt())) {
            reviewReason = "PAYMENT_EXPIRED_OR_DELIVERY_LATE";
        }

        LocalDateTime appointmentStart = LocalDateTime.of(
                appointment.getSlot().getSlotDate(),
                appointment.getSlot().getStartTime()
        );

        if (reviewReason == null
                && !appointmentStart.isAfter(receivedAt)) {
            reviewReason = "APPOINTMENT_ALREADY_STARTED";
        }

        if (reviewReason == null
                && paymentRepository
                        .existsByAppointment_IdAndStatus(
                                appointmentId,
                                PaymentStatus.SUCCESS
                        )) {
            reviewReason = "APPOINTMENT_ALREADY_PAID";
        }

        if (reviewReason != null) {
            payment.setRequiresReview(true);

            if (payment.getReviewReason() == null
                    || payment.getReviewReason().isBlank()) {
                payment.setReviewReason(
                        reviewReason + "; SePay id=" + sepayId
                );
            }

            recordEvent(
                    eventKey, fingerprint, "REVIEW", payment.getId(),
                    reviewReason, receivedAt
            );

            return;
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionNo(Long.toString(sepayId));
        payment.setBankCode(gateway);
        payment.setPaidAt(bankTime);
        payment.setRequiresReview(false);
        payment.setReviewReason(null);

        recordEvent(
                eventKey, fingerprint, "SUCCESS", payment.getId(),
                "PAYMENT_COMPLETED", receivedAt
        );

        log.info(
                "SEPAY_PAYMENT_SUCCESS paymentId={} appointmentId={} "
                        + "sepayId={} amount={}",
                payment.getId(),
                appointmentId,
                sepayId,
                amount
        );
    }

    private boolean alreadyProcessed(
            String eventKey,
            String fingerprint
    ) {
        SepayWebhookEvent existing = eventRepository
                .findById(eventKey)
                .orElse(null);

        if (existing == null) {
            return false;
        }

        if (!existing.getPayloadHash().equals(fingerprint)) {
            log.error(
                    "SEPAY_EVENT_CONFLICT eventKey={}",
                    eventKey
            );

            throw error(
                    HttpStatus.CONFLICT,
                    "ID SePay da ton tai nhung du lieu khac."
            );
        }

        log.info(
                "SEPAY_DUPLICATE eventKey={} outcome={}",
                eventKey,
                existing.getOutcome()
        );

        return true;
    }

    private void recordEvent(
            String eventKey,
            String fingerprint,
            String outcome,
            Long paymentId,
            String note,
            LocalDateTime receivedAt
    ) {
        SepayWebhookEvent event = new SepayWebhookEvent();
        event.setEventKey(eventKey);
        event.setPayloadHash(fingerprint);
        event.setOutcome(outcome);
        event.setPaymentId(paymentId);
        event.setNote(note);
        event.setReceivedAt(receivedAt);

        eventRepository.saveAndFlush(event);

        log.info(
                "SEPAY_EVENT eventKey={} outcome={} paymentId={} note={}",
                eventKey,
                outcome,
                paymentId,
                note
        );
    }

    private void collectCodes(Set<String> codes, String value) {
        var matcher = PAYMENT_CODE.matcher(value);

        while (matcher.find()) {
            codes.add(matcher.group());
        }
    }

    private String text(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : value.toString().strip();
    }

    private long positiveLong(Object value, String field) {
        try {
            if (value == null) {
                throw new NumberFormatException();
            }

            long parsed = new BigDecimal(
                    value.toString()
            ).longValueExact();

            if (parsed <= 0) {
                throw new NumberFormatException();
            }

            return parsed;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Truong " + field + " khong hop le."
            );
        }
    }

    private LocalDateTime parseBankTime(String value) {
        try {
            return LocalDateTime.parse(value, BANK_TIME);
        } catch (DateTimeParseException ex) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "transactionDate phai co dang yyyy-MM-dd HH:mm:ss."
            );
        }
    }

    private String fingerprint(List<String> values) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

                digest.update(
                        Integer.toString(bytes.length)
                                .getBytes(StandardCharsets.US_ASCII)
                );
                digest.update((byte) ':');
                digest.update(bytes);
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "Khong the tao fingerprint webhook.",
                    ex
            );
        }
    }

    private void requireReceiverConfigured() {
        if (expectedAccount == null
                || expectedAccount.isBlank()
                || expectedGateway == null
                || expectedGateway.isBlank()) {
            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chua cau hinh tai khoan nhan SePay."
            );
        }
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message
    ) {
        return new ResponseStatusException(status, message);
    }
}