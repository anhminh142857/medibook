package com.medibook.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
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
import com.medibook.backend.repository.AppointmentSlotRepository;
import com.medibook.backend.repository.PaymentTransactionRepository;
import com.medibook.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final String PATIENT_EMAIL = "patient@example.com";
    private static final Long APPOINTMENT_ID = 9L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentSlotRepository slotRepository;

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    private AppointmentService service;
    private Appointment appointment;
    private AppointmentSlot slot;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(
                appointmentRepository,
                slotRepository,
                paymentRepository,
                userRepository,
                emailNotificationService
        );

        User patient = new User();
        patient.setId(17L);
        patient.setFullName("Test Patient");
        patient.setEmail(PATIENT_EMAIL);
        patient.setStatus("ACTIVE");

        Doctor doctor = new Doctor();
        doctor.setId(22L);
        doctor.setFullName("Test Doctor");

        slot = new AppointmentSlot();
        slot.setId(33L);
        slot.setDoctor(doctor);
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.NOON);
        slot.setEndTime(LocalTime.of(13, 0));
        slot.setStatus("BOOKED");

        appointment = new Appointment();
        appointment.setId(APPOINTMENT_ID);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSlot(slot);
        appointment.setStatus("PENDING");

        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
    }

    @Test
    void cancelRejectsSuccessfulPaymentWithoutChangingAppointmentOrSlot() {
        PaymentTransaction payment = payment(PaymentStatus.SUCCESS, false);
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(payment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(PATIENT_EMAIL, APPOINTMENT_ID)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verifyNoInteractions(slotRepository, emailNotificationService);
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void cancelRejectsReviewPaymentWithoutChangingAppointmentOrSlot() {
        PaymentTransaction payment = payment(PaymentStatus.PENDING, true);
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(payment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(PATIENT_EMAIL, APPOINTMENT_ID)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verifyNoInteractions(slotRepository, emailNotificationService);
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void cancelAllowsAnUnpaidPendingAppointmentWhenExistingRulesPass() {
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of());
        when(slotRepository.findByIdForUpdate(slot.getId()))
                .thenReturn(Optional.of(slot));
        when(appointmentRepository.save(appointment))
                .thenReturn(appointment);
        when(slotRepository.save(slot)).thenReturn(slot);

        var result = service.cancelAppointment(PATIENT_EMAIL, APPOINTMENT_ID);

        assertEquals("CANCELLED", result.getStatus());
        assertEquals("CANCELLED", appointment.getStatus());
        assertEquals("AVAILABLE", slot.getStatus());
        verify(emailNotificationService).sendAfterCommit(
                eq(PATIENT_EMAIL),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void cancelRejectsAppointmentOwnedByAnotherPatient() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(
                        "another@example.com",
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoInteractions(
                paymentRepository,
                slotRepository,
                emailNotificationService
        );
    }

    @Test
    void cancelRejectsAppointmentThatIsNotPending() {
        appointment.setStatus("CONFIRMED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(PATIENT_EMAIL, APPOINTMENT_ID)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("CONFIRMED", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoInteractions(
                paymentRepository,
                slotRepository,
                emailNotificationService
        );
    }

    @Test
    void cancelRejectsAppointmentWhoseSlotIsNotBooked() {
        slot.setStatus("AVAILABLE");
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of());
        when(slotRepository.findByIdForUpdate(slot.getId()))
                .thenReturn(Optional.of(slot));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(PATIENT_EMAIL, APPOINTMENT_ID)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("AVAILABLE", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verify(slotRepository, never()).save(slot);
        verify(emailNotificationService, never()).sendAfterCommit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private PaymentTransaction payment(
            PaymentStatus status,
            boolean requiresReview
    ) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setAppointment(appointment);
        payment.setTxnRef("test-reference");
        payment.setAmount(100_000L);
        payment.setStatus(status);
        payment.setRequiresReview(requiresReview);
        payment.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        return payment;
    }
}
