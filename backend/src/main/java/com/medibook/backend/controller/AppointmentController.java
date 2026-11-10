package com.medibook.backend.controller;

import com.medibook.backend.dto.AppointmentRequest;
import com.medibook.backend.dto.AppointmentResponse;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.UserRepository;
import com.medibook.backend.service.AppointmentQueryService;
import com.medibook.backend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentQueryService appointmentQueryService;
    private final UserRepository userRepository;

    public AppointmentController(
            AppointmentService appointmentService,
            AppointmentQueryService appointmentQueryService,
            UserRepository userRepository
    ) {
        this.appointmentService = appointmentService;
        this.appointmentQueryService = appointmentQueryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            Principal principal
    ) {
        List<AppointmentResponse> appointments =
                appointmentQueryService.getMyAppointments(
                        principal.getName()
                );

        return ResponseEntity.ok(appointments);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            Principal principal,
            @Valid @RequestBody AppointmentRequest request
    ) {
        User patient = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Tai khoan khong ton tai."
                ));

        AppointmentResponse response =
                appointmentService.createAppointment(
                        patient.getId(),
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            Principal principal,
            @PathVariable Long appointmentId
    ) {
        AppointmentResponse response =
                appointmentService.cancelAppointment(
                        principal.getName(),
                        appointmentId
                );

        return ResponseEntity.ok(response);
    }
}