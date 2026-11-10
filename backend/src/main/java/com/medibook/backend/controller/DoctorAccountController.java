package com.medibook.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.backend.service.DoctorAccountService;
import com.medibook.backend.service.DoctorAccountService.DoctorAccountResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/admin/doctors")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class DoctorAccountController {

    private final DoctorAccountService doctorAccountService;

    public DoctorAccountController(
            DoctorAccountService doctorAccountService
    ) {
        this.doctorAccountService = doctorAccountService;
    }

    @PostMapping("/{doctorId}/account")
    public ResponseEntity<DoctorAccountResponse> linkAccount(
            @PathVariable("doctorId") Long doctorId,
            @Valid @RequestBody LinkDoctorAccountRequest request
    ) {
        DoctorAccountResponse response =
                doctorAccountService.linkAccount(
                        doctorId,
                        request.email()
                );

        return ResponseEntity.ok(response);
    }

    public record LinkDoctorAccountRequest(
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không hợp lệ")
            @Size(
                    max = 255,
                    message = "Email không quá 255 ký tự"
            )
            String email
    ) {
    }
}