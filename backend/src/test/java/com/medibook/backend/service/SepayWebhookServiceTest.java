package com.medibook.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.entity.PaymentStatus;
import com.medibook.backend.entity.PaymentTransaction;
import com.medibook.backend.entity.SepayWebhookEvent;
import com.medibook.backend.repository.AppointmentRepository;
import com.medibook.backend.repository.PaymentTransactionRepository;
import com.medibook.backend.repository.SepayWebhookEventRepository;

@ExtendWith(MockitoExtension.class)
class SepayWebhookServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter BANK_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private static final String EVENT_KEY = "sandbox:53853";
    private static final String TXN_REF = "BAM00000003";
    private static final long APPOINTMENT_ID = 9L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private SepayWebhookEventRepository eventRepository;

    @Captor
    private ArgumentCaptor<SepayWebhookEvent> eventCaptor;

    private SepayWebhookService service;
    private Appointment appointment;
    private PaymentTransaction payment;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        service = new SepayWebhookService(
                appointmentRepository,
                paymentRepository,
                eventRepository,
                "1234567890",
                "ACB"
        );

        now = LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS);

        AppointmentSlot slot = new AppointmentSlot();
        slot.setSlotDate(LocalDate.now(ZONE).plusDays(2));
        slot.setStartTime(LocalTime.NOON);

        appointment = new Appointment();
        appointment.setId(APPOINTMENT_ID);
        appointment.setSlot(slot);
        appointment.setStatus("PENDING");

        payment = new PaymentTransaction();
        payment.setId(3L);
        payment.setAppointment(appointment);
        payment.setTxnRef(TXN_REF);
        payment.setAmount(100_000L);
        payment.setCurrency("VND");
        payment.setProvider("SEPAY");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(now.minusHours(1));
        payment.setExpiresAt(now.plusMinutes(30));
    }

    @Test
    void validPendingPaymentBecomesSuccessfulAndRecordsEvent() {
        stubFreshProcessing();
        when(paymentRepository.existsByAppointment_IdAndStatus(
                APPOINTMENT_ID,
                PaymentStatus.SUCCESS
        )).thenReturn(false);

        service.handle(payload("in", 100_000L, "BAM00000003"));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(bankTime(), payment.getPaidAt());
        assertEquals("53853", payment.getGatewayTransactionNo());
        assertEquals("ACB", payment.getBankCode());
        assertFalse(payment.isRequiresReview());
        assertNull(payment.getReviewReason());

        verify(eventRepository, times(2)).findById(EVENT_KEY);
        verify(eventRepository).saveAndFlush(eventCaptor.capture());
        SepayWebhookEvent event = eventCaptor.getValue();
        assertEquals("SUCCESS", event.getOutcome());
        assertEquals(3L, event.getPaymentId());
        assertEquals("PAYMENT_COMPLETED", event.getNote());
    }

    @Test
    void sameProcessedIdAndFingerprintDoesNotSaveAnotherEventOrChangePayment() {
        stubFreshProcessing();
        Map<String, Object> payload =
                payload("in", 100_000L, "BAM00000003");

        service.handle(payload);
        verify(eventRepository).saveAndFlush(eventCaptor.capture());
        SepayWebhookEvent processedEvent = eventCaptor.getValue();

        PaymentStatus statusBeforeReplay = payment.getStatus();
        LocalDateTime paidAtBeforeReplay = payment.getPaidAt();
        String gatewayNumberBeforeReplay =
                payment.getGatewayTransactionNo();
        doReturn(Optional.of(processedEvent))
                .when(eventRepository).findById(EVENT_KEY);

        service.handle(payload);

        assertEquals(statusBeforeReplay, payment.getStatus());
        assertEquals(paidAtBeforeReplay, payment.getPaidAt());
        assertEquals(
                gatewayNumberBeforeReplay,
                payment.getGatewayTransactionNo()
        );
        verify(eventRepository, times(1)).saveAndFlush(
                org.mockito.ArgumentMatchers.any(SepayWebhookEvent.class)
        );
    }

    @Test
    void sameIdWithDifferentFingerprintReturnsConflictWithoutProcessingPayment() {
        SepayWebhookEvent existing = event("different-fingerprint", "SUCCESS");
        when(eventRepository.findById(EVENT_KEY))
                .thenReturn(Optional.of(existing));
        Map<String, Object> payload =
                payload("in", 100_000L, "BAM00000003");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.handle(payload)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaidAt());
        verifyNoInteractions(appointmentRepository, paymentRepository);
        verify(eventRepository).findById(EVENT_KEY);
    }

    @Test
    void underpaymentRequiresReviewAndRecordsAmountMismatch() {
        assertAmountMismatch(99_999L);
    }

    @Test
    void overpaymentRequiresReviewAndRecordsAmountMismatch() {
        assertAmountMismatch(100_001L);
    }

    @Test
    void expiredPaymentWithOtherwiseValidTransferRequiresLatePaymentReview() {
        stubFreshProcessing();
        payment.setCreatedAt(now.minusHours(2));
        payment.setExpiresAt(now.minusHours(1));

        service.handle(payload(
                "in",
                100_000L,
                "BAM00000003",
                now.minusMinutes(90)
        ));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaidAt());
        assertTrue(payment.isRequiresReview());
        assertTrue(payment.getReviewReason().startsWith(
                "PAYMENT_EXPIRED_OR_DELIVERY_LATE"
        ));
        assertEvent("REVIEW", "PAYMENT_EXPIRED_OR_DELIVERY_LATE");
    }

    @Test
    void additionalTransferAfterSuccessKeepsOriginalPaymentDetailsAndRequiresReview() {
        stubFreshProcessing();
        LocalDateTime originalPaidAt = now.minusDays(1);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(originalPaidAt);
        payment.setGatewayTransactionNo("OLD-GATEWAY-TXN");

        service.handle(payload("in", 100_000L, "BAM00000003"));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(originalPaidAt, payment.getPaidAt());
        assertEquals("OLD-GATEWAY-TXN", payment.getGatewayTransactionNo());
        assertTrue(payment.isRequiresReview());
        assertTrue(payment.getReviewReason().startsWith(
                "ADDITIONAL_TRANSFER_AFTER_SUCCESS"
        ));
        assertEvent("REVIEW", "ADDITIONAL_TRANSFER_AFTER_SUCCESS");
    }

    @Test
    void wrongReceiverIsIgnoredWithoutUpdatingPayment() {
        stubNoExistingEvent();
        Map<String, Object> payload =
                payload("in", 100_000L, "BAM00000003");
        payload.put("accountNumber", "different-account");

        service.handle(payload);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaidAt());
        verify(paymentRepository, times(0))
                .findAppointmentIdByTxnRef(TXN_REF);
        assertEvent("IGNORED", "RECEIVER_MISMATCH");
    }

    @Test
    void wrongBankIsIgnoredWithoutUpdatingPayment() {
        stubNoExistingEvent();
        Map<String, Object> payload =
                payload("in", 100_000L, "BAM00000003");
        payload.put("gateway", "different-bank");

        service.handle(payload);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaidAt());
        verify(paymentRepository, times(0))
                .findAppointmentIdByTxnRef(TXN_REF);
        assertEvent("IGNORED", "RECEIVER_MISMATCH");
    }

    @Test
    void outgoingTransferIsIgnored() {
        stubNoExistingEvent();

        service.handle(payload("out", 100_000L, "BAM00000003"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaidAt());
        verify(paymentRepository, times(0))
                .findAppointmentIdByTxnRef(TXN_REF);
        assertEvent("IGNORED", "OUTGOING_TRANSFER");
    }

    @Test
    void missingPaymentCodeIsUnmatched() {
        stubNoExistingEvent();

        service.handle(payload("in", 100_000L, ""));

        assertEvent("UNMATCHED", "NO_PAYMENT_CODE");
        verify(paymentRepository, times(0))
                .findAppointmentIdByTxnRef(TXN_REF);
    }

    @Test
    void multiplePaymentCodesAreUnmatched() {
        stubNoExistingEvent();
        Map<String, Object> payload = payload("in", 100_000L, "");
        payload.put("content", "BAM00000003 BAM00000004");

        service.handle(payload);

        assertEvent("UNMATCHED", "MULTIPLE_PAYMENT_CODES");
        verify(paymentRepository, times(0))
                .findAppointmentIdByTxnRef(TXN_REF);
    }

    @Test
    void unknownPaymentCodeIsUnmatchedAsPaymentNotFound() {
        stubNoExistingEvent();
        when(paymentRepository.findAppointmentIdByTxnRef("BAM00000004"))
                .thenReturn(Optional.empty());

        service.handle(payload("in", 100_000L, "BAM00000004"));

        assertEvent("UNMATCHED", "PAYMENT_NOT_FOUND");
    }

    @Test
    void paymentAlreadyMarkedForReviewIsNotAutomaticallyConfirmed() {
        stubFreshProcessing();
        payment.setRequiresReview(true);
        payment.setReviewReason("MANUAL_CHECK");

        service.handle(payload("in", 100_000L, "BAM00000003"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaidAt());
        assertTrue(payment.isRequiresReview());
        assertEquals("MANUAL_CHECK", payment.getReviewReason());
        assertEvent("REVIEW", "PAYMENT_ALREADY_REQUIRES_REVIEW");
    }

    private void assertAmountMismatch(long transferAmount) {
        stubFreshProcessing();

        service.handle(payload(
                "in",
                transferAmount,
                "BAM00000003"
        ));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaidAt());
        assertTrue(payment.isRequiresReview());
        assertTrue(payment.getReviewReason().startsWith("AMOUNT_MISMATCH"));
        assertEvent("REVIEW", "AMOUNT_MISMATCH");
    }

    private void stubFreshProcessing() {
        when(eventRepository.findById(EVENT_KEY))
                .thenReturn(Optional.empty(), Optional.empty());
        when(paymentRepository.findAppointmentIdByTxnRef(TXN_REF))
                .thenReturn(Optional.of(APPOINTMENT_ID));
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
        when(paymentRepository.findByTxnRefForUpdate(TXN_REF))
                .thenReturn(Optional.of(payment));
    }

    private void stubNoExistingEvent() {
        when(eventRepository.findById(EVENT_KEY))
                .thenReturn(Optional.empty());
    }

    private void assertEvent(String outcome, String note) {
        verify(eventRepository).saveAndFlush(eventCaptor.capture());
        SepayWebhookEvent event = eventCaptor.getValue();
        assertEquals(outcome, event.getOutcome());
        assertEquals(note, event.getNote());
    }

    private SepayWebhookEvent event(String payloadHash, String outcome) {
        SepayWebhookEvent event = new SepayWebhookEvent();
        event.setEventKey(EVENT_KEY);
        event.setPayloadHash(payloadHash);
        event.setOutcome(outcome);
        event.setNote("existing");
        event.setReceivedAt(now);
        return event;
    }

    private Map<String, Object> payload(
            String transferType,
            long amount,
            String paymentCode
    ) {
        return payload(transferType, amount, paymentCode, now.minusMinutes(5));
    }

    private Map<String, Object> payload(
            String transferType,
            long amount,
            String paymentCode,
            LocalDateTime transactionTime
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", 53853L);
        payload.put("transferAmount", amount);
        payload.put("transferType", transferType);
        payload.put("accountNumber", "1234567890");
        payload.put("gateway", "ACB");
        payload.put("code", paymentCode);
        payload.put("content", paymentCode);
        payload.put(
                "transactionDate",
                transactionTime.format(BANK_TIME)
        );
        return payload;
    }

    private LocalDateTime bankTime() {
        return now.minusMinutes(5).truncatedTo(ChronoUnit.SECONDS);
    }
}
