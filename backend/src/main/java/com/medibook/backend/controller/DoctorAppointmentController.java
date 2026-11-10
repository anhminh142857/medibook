package com.medibook.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.backend.dto.AppointmentResponse;
import com.medibook.backend.service.DoctorAppointmentService;

@RestController
@RequestMapping("/api/doctor/appointments")
@PreAuthorize("hasAuthority('ROLE_DOCTOR')")
public class DoctorAppointmentController {

    private final DoctorAppointmentService doctorAppointmentService;

    public DoctorAppointmentController(
            DoctorAppointmentService doctorAppointmentService
    ) {
        this.doctorAppointmentService = doctorAppointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            Principal principal
    ) {
        List<AppointmentResponse> appointments =
                doctorAppointmentService.getMyAppointments(
                        principal.getName()
                );

        return ResponseEntity.ok(appointments);
    }

    @PostMapping("/{appointmentId}/confirm")
    public ResponseEntity<AppointmentResponse> confirmAppointment(
            @PathVariable("appointmentId") Long appointmentId,
            Principal principal
    ) {
        AppointmentResponse response =
                doctorAppointmentService.confirmAppointment(
                        principal.getName(),
                        appointmentId
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable("appointmentId") Long appointmentId,
            Principal principal
    ) {
        AppointmentResponse response =
                doctorAppointmentService.completeAppointment(
                        principal.getName(),
                        appointmentId
                );

        return ResponseEntity.ok(response);
    }
}