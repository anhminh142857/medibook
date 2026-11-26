package com.medibook.backend.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.dto.SepayPaymentResponse;
import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.PaymentStatus;
import com.medibook.backend.entity.PaymentTransaction;
import com.medibook.backend.repository.AppointmentRepository;
import com.medibook.backend.repository.PaymentTransactionRepository;

@Service
public class SepayPaymentService {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final AppointmentRepository appointmentRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final boolean paymentEnabled;
    private final long paymentAmount;
    private final int expireMinutes;
    private final String receiverAccount;
    private final String receiverGateway;

    public SepayPaymentService(
            AppointmentRepository appointmentRepository,
            PaymentTransactionRepository paymentRepository,
            @Value("${app.sepay.payment-enabled:false}")
            boolean paymentEnabled,
            @Value("${app.sepay.payment-amount:0}")
            long paymentAmount,
            @Value("${app.sepay.payment-expire-minutes:15}")
            int expireMinutes,
            @Value("${app.sepay.receiver-account:}")
            String receiverAccount,
            @Value("${app.sepay.receiver-gateway:}")
            String receiverGateway
    ) {
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.paymentEnabled = paymentEnabled;
        this.paymentAmount = paymentAmount;
        this.expireMinutes = expireMinutes;
        this.receiverAccount = receiverAccount;
        this.receiverGateway = receiverGateway;
    }

    @Transactional
    public SepayPaymentResponse createPayment(
            String patientEmail,
            Long appointmentId
    ) {
        if (appointmentId == null || appointmentId <= 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Ma lich hen khong hop le."
            );
        }

        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Lich hen khong ton tai."
                ));

        requireOwner(appointment, patientEmail);

        requireConfigured();

        if (!"ACTIVE".equals(
                appointment.getPatient().getStatus()
        )) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan khong hoat dong."
            );
        }

        if (!"PENDING".equals(appointment.getStatus())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Ban thu nay chi cho thanh toan lich dang cho xac nhan."
            );
        }

        LocalDateTime now = LocalDateTime.now(ZONE);

        LocalDateTime appointmentStart = LocalDateTime.of(
                appointment.getSlot().getSlotDate(),
                appointment.getSlot().getStartTime()
        );

        if (!appointmentStart.isAfter(now)) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Lich hen da den gio kham."
            );
        }

        if (paymentRepository.existsByAppointment_IdAndStatus(
                appointmentId,
                PaymentStatus.SUCCESS
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Lich hen da duoc thanh toan."
            );
        }

        var previousPayments = paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        appointmentId
                );

        boolean needsReview = previousPayments.stream()
                .anyMatch(PaymentTransaction::isRequiresReview);

        if (needsReview) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Lich hen co giao dich can kiem tra. Chua tao thanh toan moi."
            );
        }

        var activePayments = previousPayments.stream()
                .filter(payment ->
                        payment.getStatus() == PaymentStatus.PENDING
                                && payment.getExpiresAt().isAfter(now)
                )
                .toList();

        if (activePayments.size() > 1) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Co nhieu giao dich dang cho. Can kiem tra truoc khi tiep tuc."
            );
        }

        if (!activePayments.isEmpty()) {
            PaymentTransaction existing = activePayments.get(0);

            if (!"SEPAY".equals(existing.getProvider())) {
                throw error(
                        HttpStatus.CONFLICT,
                        "Lich hen dang co giao dich cua cong thanh toan khac."
                );
            }

            return toResponse(existing);
        }

        LocalDateTime expiresAt = now.plusMinutes(expireMinutes);

        if (expiresAt.isAfter(appointmentStart)) {
            expiresAt = appointmentStart;
        }

        PaymentTransaction payment = new PaymentTransaction();
        payment.setAppointment(appointment);
        payment.setProvider("SEPAY");
        payment.setCurrency("VND");
        payment.setAmount(paymentAmount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExpiresAt(expiresAt);

        // Ma tam chi ton tai trong transaction tao giao dich.
        payment.setTxnRef("TMP" + UUID.randomUUID()
                .toString()
                .replace("-", ""));

        paymentRepository.saveAndFlush(payment);

        Long paymentId = payment.getId();

        if (paymentId == null
                || paymentId <= 0
                || paymentId > 99_999_999L) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Khong the sinh ma BAM 8 chu so."
            );
        }

        String txnRef = String.format(
                Locale.ROOT,
                "BAM%08d",
                paymentId
        );

        if (paymentRepository.findByTxnRef(txnRef).isPresent()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Ma thanh toan da ton tai. Can kiem tra du lieu."
            );
        }

        payment.setTxnRef(txnRef);
        paymentRepository.saveAndFlush(payment);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public SepayPaymentResponse getPayment(
            String patientEmail,
            Long paymentId
    ) {
        if (paymentId == null || paymentId <= 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Ma giao dich khong hop le."
            );
        }

        PaymentTransaction payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Giao dich khong ton tai."
                ));

        requireOwner(payment.getAppointment(), patientEmail);

        if (!"SEPAY".equals(payment.getProvider())) {
            throw error(
                    HttpStatus.NOT_FOUND,
                    "Khong phai giao dich SePay."
            );
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<SepayPaymentResponse> getPaymentsForAppointment(
            String patientEmail,
            Long appointmentId
    ) {
        if (appointmentId == null || appointmentId <= 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Ma lich hen khong hop le."
            );
        }

        Appointment appointment = appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Lich hen khong ton tai."
                ));

        requireOwner(appointment, patientEmail);

        return paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        appointmentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireConfigured() {
        if (!paymentEnabled
                || paymentAmount <= 0
                || expireMinutes <= 0
                || expireMinutes > 60
                || receiverAccount == null
                || receiverAccount.isBlank()
                || receiverGateway == null
                || receiverGateway.isBlank()) {
            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Thanh toan SePay chua duoc cau hinh hop le."
            );
        }
    }

    private void requireOwner(
            Appointment appointment,
            String patientEmail
    ) {
        if (patientEmail == null
                || !appointment.getPatient()
                        .getEmail()
                        .equals(patientEmail)) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Ban khong duoc truy cap thanh toan cua nguoi khac."
            );
        }
    }

    private SepayPaymentResponse toResponse(
            PaymentTransaction payment
    ) {
        return new SepayPaymentResponse(
                payment.getId(),
                payment.getAppointment().getId(),
                payment.getTxnRef(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getStatus().name(),
                payment.getTxnRef(),
                payment.getExpiresAt(),
                payment.getPaidAt(),
                payment.isRequiresReview(),
                payment.getReviewReason(),
                "SEPAY".equals(payment.getProvider())
                        ? receiverAccount
                        : null,
                "SEPAY".equals(payment.getProvider())
                        ? receiverGateway
                        : null
        );
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message
    ) {
        return new ResponseStatusException(status, message);
    }
}