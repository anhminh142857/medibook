package com.medibook.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.dto.AppointmentResponse;
import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.entity.Doctor;
import com.medibook.backend.entity.PaymentStatus;
import com.medibook.backend.entity.PaymentTransaction;
import com.medibook.backend.entity.Role;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.AppointmentRepository;
import com.medibook.backend.repository.DoctorRepository;
import com.medibook.backend.repository.PaymentTransactionRepository;

@ExtendWith(MockitoExtension.class)
class DoctorAppointmentServiceTest {

    private static final String DOCTOR_EMAIL = "doctor@example.com";
    private static final Long APPOINTMENT_ID = 9L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    private DoctorAppointmentService service;
    private Appointment appointment;
    private AppointmentSlot slot;

    @BeforeEach
    void setUp() {
        service = new DoctorAppointmentService(
                appointmentRepository,
                doctorRepository,
                paymentRepository,
                emailNotificationService
        );

        User doctorUser = new User();
        doctorUser.setId(3L);
        doctorUser.setEmail(DOCTOR_EMAIL);
        doctorUser.setStatus("ACTIVE");
        doctorUser.getRoles().add(new Role("ROLE_DOCTOR"));

        Doctor doctor = new Doctor();
        doctor.setId(22L);
        doctor.setFullName("Test Doctor");
        doctor.setUser(doctorUser);

        User patient = new User();
        patient.setId(17L);
        patient.setFullName("Test Patient");
        patient.setEmail("patient@example.com");

        slot = new AppointmentSlot();
        slot.setId(33L);
        slot.setSlotDate(LocalDate.of(2030, 1, 15));
        slot.setStartTime(LocalTime.NOON);
        slot.setEndTime(LocalTime.of(13, 0));
        slot.setStatus("BOOKED");

        appointment = new Appointment();
        appointment.setId(APPOINTMENT_ID);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSlot(slot);
        appointment.setStatus("PENDING");

        when(doctorRepository.findByUserEmail(DOCTOR_EMAIL))
                .thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByIdForUpdate(APPOINTMENT_ID))
                .thenReturn(Optional.of(appointment));
    }

    @Test
    void confirmAllowsAValidSuccessfulSepayPayment() {
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(payment(
                        PaymentStatus.SUCCESS,
                        false,
                        "SEPAY"
                )));
        when(appointmentRepository.save(appointment))
                .thenReturn(appointment);

        var result = service.confirmAppointment(
                DOCTOR_EMAIL,
                APPOINTMENT_ID
        );

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(emailNotificationService).sendAfterCommit(
                eq("patient@example.com"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void confirmRejectsWhenNoSuccessfulPaymentExists() {
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(payment(
                        PaymentStatus.PENDING,
                        false,
                        "SEPAY"
                )));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verify(emailNotificationService, never()).sendAfterCommit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void confirmRejectsCleanSuccessWhenAnotherPaymentNeedsReview() {
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(
                        payment(PaymentStatus.SUCCESS, false, "SEPAY"),
                        payment(PaymentStatus.PENDING, true, "SEPAY")
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verify(emailNotificationService, never()).sendAfterCommit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void confirmRejectsSuccessfulPaymentFromAnotherProvider() {
        when(paymentRepository
                .findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
                        APPOINTMENT_ID
                ))
                .thenReturn(List.of(payment(
                        PaymentStatus.SUCCESS,
                        false,
                        "VNPAY"
                )));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
    }

    @Test
    void confirmRejectsAppointmentOwnedByAnotherDoctor() {
        appointment.setDoctor(anotherDoctor());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void confirmRejectsAppointmentThatIsNotPending() {
        appointment.setStatus("CANCELLED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("CANCELLED", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void confirmRejectsAppointmentWhoseSlotIsNotBooked() {
        slot.setStatus("AVAILABLE");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("AVAILABLE", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void completeRejectsAppointmentOwnedByAnotherDoctor() {
        appointment.setDoctor(anotherDoctor());
        appointment.setStatus("CONFIRMED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.completeAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("CONFIRMED", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void completeRejectsAppointmentThatIsNotConfirmed() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.completeAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void completeRejectsAppointmentWhoseSlotIsNotBooked() {
        appointment.setStatus("CONFIRMED");
        slot.setStatus("AVAILABLE");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.completeAppointment(
                        DOCTOR_EMAIL,
                        APPOINTMENT_ID
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("CONFIRMED", appointment.getStatus());
        assertEquals("AVAILABLE", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void completeRejectsBeforeTheSlotEndTime() {
        appointment.setStatus("CONFIRMED");
        LocalDateTime endDateTime = LocalDateTime.of(
                slot.getSlotDate(),
                slot.getEndTime()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> completeAt(endDateTime.minusNanos(1))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("CONFIRMED", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository, never()).save(appointment);
        verifyNoNotifications();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void completeAllowsAtTheSlotEndTime() {
        assertCanCompleteAt(LocalDateTime.of(
                slot.getSlotDate(),
                slot.getEndTime()
        ));
    }

    @Test
    void completeAllowsAfterTheSlotEndTime() {
        assertCanCompleteAt(LocalDateTime.of(
                slot.getSlotDate(),
                slot.getEndTime()
        ).plusNanos(1));
    }

    private void assertCanCompleteAt(LocalDateTime now) {
        appointment.setStatus("CONFIRMED");
        when(appointmentRepository.save(appointment))
                .thenReturn(appointment);

        AppointmentResponse result = completeAt(now);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("COMPLETED", appointment.getStatus());
        assertEquals("BOOKED", slot.getStatus());
        verify(appointmentRepository).save(appointment);
        verify(emailNotificationService).sendAfterCommit(
                eq("patient@example.com"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private AppointmentResponse completeAt(LocalDateTime now) {
        try (MockedStatic<LocalDateTime> localDateTime =
                     mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            localDateTime.when(LocalDateTime::now).thenReturn(now);
            return service.completeAppointment(DOCTOR_EMAIL, APPOINTMENT_ID);
        }
    }

    private Doctor anotherDoctor() {
        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(23L);
        otherDoctor.setFullName("Another Doctor");
        return otherDoctor;
    }

    private void verifyNoNotifications() {
        verify(emailNotificationService, never()).sendAfterCommit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private PaymentTransaction payment(
            PaymentStatus status,
            boolean requiresReview,
            String provider
    ) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setAppointment(appointment);
        payment.setTxnRef("test-reference");
        payment.setAmount(100_000L);
        payment.setStatus(status);
        payment.setProvider(provider);
        payment.setRequiresReview(requiresReview);
        payment.setExpiresAt(LocalDateTime.of(2030, 1, 15, 10, 0)
                .plusMinutes(10));
        return payment;
    }
}
