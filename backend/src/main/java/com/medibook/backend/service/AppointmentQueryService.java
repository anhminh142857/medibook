package com.medibook.backend.service;

import com.medibook.backend.dto.AppointmentResponse;
import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentQueryService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentQueryService(
            AppointmentRepository appointmentRepository
    ) {
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(
            String patientEmail
    ) {
        return appointmentRepository
                .findAllForPatientEmail(patientEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AppointmentResponse toResponse(
            Appointment appointment
    ) {
        AppointmentSlot slot = appointment.getSlot();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                slot.getId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                appointment.getStatus()
        );
    }
}