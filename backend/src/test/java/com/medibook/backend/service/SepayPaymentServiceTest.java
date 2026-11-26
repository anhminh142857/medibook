package com.medibook.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.entity.Doctor;
import com.medibook.backend.entity.PaymentStatus;
import com.medibook.backend.entity.PaymentTransaction;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.AppointmentRepository;
import com.medibook.backend.repository.PaymentTransactionRepository;

@ExtendWith(MockitoExtension.class)
class SepayPaymentServiceTest {

    private static final String PATIENT_EMAIL = "patient@example.com";
    private static final Long APPOINTMENT_ID = 9L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PaymentTransactionRepository paymentRepository;

    private SepayPaymentService service;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new SepayPaymentService(
                appointmentRepository,
                paymentRepository,
                true,
                100_000L,
                15,
                "configured-receiver",
                "configured-bank"
        );

        User patient = new User();
        patient.setId(17L);
        patient.setEmail(PATIENT_EMAIL);
        patient.setStatus("ACTIVE");

        AppointmentSlot slot = new AppointmentSlot();
        slot.setSlotDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .plusDays(2));
        slot.setStartTime(LocalTime.NOON);

        appointment = new Appointment();
        appointment.setId(APPOINTMENT_ID);
        appointment.setPatient(patient);
        appointment.setDoctor(new Doctor());
        appointment.setSlot(slot);
        appointment.setStatus("PENDING");
    }

    @Test
    void createPaymentReusesAnActiveSePayTransaction() {
        PaymentTransaction existing = payment(
                3L,
                PaymentStatus.PENDING,
                false,
                LocalDateTime.now().plusMinutes(10)
        );
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
        when(paymentRepository.existsByAppointment_IdAndStatus(
                APPOINTMENT_ID,
                PaymentStatus.SUCCESS
        )).thenReturn(false);
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(existing));

        var result = service.createPayment(PATIENT_EMAIL, APPOINTMENT_ID);

        assertEquals(3L, result.paymentId());
        assertEquals("configured-receiver", result.receiverAccount());
        assertEquals("configured-bank", result.receiverGateway());
        verify(paymentRepository, never()).saveAndFlush(
                org.mockito.ArgumentMatchers.any(PaymentTransaction.class)
        );
    }

    @Test
    void createPaymentRejectsARequestFromAnotherPatient() {
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createPayment("other@example.com", APPOINTMENT_ID)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentChecksOwnershipBeforeConfiguration() {
        SepayPaymentService disabledService = new SepayPaymentService(
                appointmentRepository,
                paymentRepository,
                false,
                0L,
                15,
                "",
                ""
        );
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> disabledService.createPayment(
                        "other@example.com",
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentRejectsAnyTransactionRequiringReview() {
        PaymentTransaction reviewed = payment(
                4L,
                PaymentStatus.PENDING,
                true,
                LocalDateTime.now().plusMinutes(10)
        );
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
        when(paymentRepository.existsByAppointment_IdAndStatus(
                APPOINTMENT_ID,
                PaymentStatus.SUCCESS
        )).thenReturn(false);
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(reviewed));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createPayment(PATIENT_EMAIL, APPOINTMENT_ID)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(paymentRepository, never()).saveAndFlush(
                org.mockito.ArgumentMatchers.any(PaymentTransaction.class)
        );
    }

    @Test
    void getPaymentsForAppointmentReturnsEmptyListWithoutCreatingPayment() {
        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of());

        var result = service.getPaymentsForAppointment(
                PATIENT_EMAIL,
                APPOINTMENT_ID
        );

        assertTrue(result.isEmpty());
        verify(paymentRepository, never()).saveAndFlush(
                org.mockito.ArgumentMatchers.any(PaymentTransaction.class)
        );
    }

    @Test
    void getPaymentsForAppointmentRejectsAnotherPatientsRequest() {
        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getPaymentsForAppointment(
                        "other@example.com",
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getPaymentsForAppointmentReturnsOrderedTransactionsForTheAppointment() {
        PaymentTransaction newest = payment(
                5L,
                PaymentStatus.SUCCESS,
                false,
                LocalDateTime.now().plusMinutes(10)
        );
        newest.setPaidAt(LocalDateTime.now());
        PaymentTransaction older = payment(
                4L,
                PaymentStatus.PENDING,
                false,
                LocalDateTime.now().plusMinutes(10)
        );
        older.setProvider("VNPAY");
        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(newest, older));

        var result = service.getPaymentsForAppointment(
                PATIENT_EMAIL,
                APPOINTMENT_ID
        );

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).paymentId());
        assertEquals(PaymentStatus.SUCCESS.name(), result.get(0).status());
        assertEquals("BAM00000005", result.get(0).transferContent());
        assertEquals("configured-receiver", result.get(0).receiverAccount());
        assertEquals("VNPAY", result.get(1).provider());
        assertNull(result.get(1).receiverAccount());
        verify(paymentRepository)
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                );
    }

    private PaymentTransaction payment(
            Long id,
            PaymentStatus status,
            boolean requiresReview,
            LocalDateTime expiresAt
    ) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setId(id);
        payment.setAppointment(appointment);
        payment.setTxnRef(String.format("BAM%08d", id));
        payment.setAmount(100_000L);
        payment.setProvider("SEPAY");
        payment.setStatus(status);
        payment.setExpiresAt(expiresAt);
        payment.setRequiresReview(requiresReview);
        return payment;
    }
}
