package com.medibook.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.backend.dto.AppointmentSlotRequest;
import com.medibook.backend.dto.AppointmentSlotResponse;
import com.medibook.backend.service.AppointmentSlotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointment-slots")
public class AppointmentSlotController {

    private final AppointmentSlotService appointmentSlotService;

    public AppointmentSlotController(
            AppointmentSlotService appointmentSlotService
    ) {
        this.appointmentSlotService = appointmentSlotService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentSlotResponse>> getAllSlots() {
        List<AppointmentSlotResponse> slots =
                appointmentSlotService.getAllSlots();

        return ResponseEntity.ok(slots);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AppointmentSlotResponse> createSlot(
            @Valid @RequestBody AppointmentSlotRequest request
    ) {
        AppointmentSlotResponse response =
                appointmentSlotService.createSlot(request);

        return ResponseEntity.ok(response);
    }
}