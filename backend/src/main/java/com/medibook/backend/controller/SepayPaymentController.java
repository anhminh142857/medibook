package com.medibook.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.dto.SepayPaymentResponse;
import com.medibook.backend.service.SepayPaymentService;

@RestController
@RequestMapping("/api/payments/sepay")
public class SepayPaymentController {

    private final SepayPaymentService paymentService;

    public SepayPaymentController(
            SepayPaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @PostMapping("/appointments/{appointmentId}")
    public ResponseEntity<SepayPaymentResponse> createPayment(
            Principal principal,
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                paymentService.createPayment(
                        requireEmail(principal),
                        appointmentId
                )
        );
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<List<SepayPaymentResponse>> getAppointmentPayments(
            Principal principal,
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentsForAppointment(
                        requireEmail(principal),
                        appointmentId
                )
        );
    }

    @GetMapping("/transactions/{paymentId}")
    public ResponseEntity<SepayPaymentResponse> getPayment(
            Principal principal,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.getPayment(
                        requireEmail(principal),
                        paymentId
                )
        );
    }

    private String requireEmail(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui long dang nhap."
            );
        }

        return principal.getName();
    }
}