package com.medibook.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.backend.dto.AuthResponse;
import com.medibook.backend.dto.LoginRequest;
import com.medibook.backend.dto.RegisterRequest;
import com.medibook.backend.entity.User;
import com.medibook.backend.service.AuthService;
import com.medibook.backend.service.GoogleAuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record GoogleLoginRequest(
            @NotBlank @Size(max = 4096) String credential
    ) {
    }

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(
            AuthService authService,
            GoogleAuthService googleAuthService
    ) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User savedUser = authService.registerPatient(request);

        List<String> roles = savedUser.getRoles().stream()
                .map(role -> role.getName())
                .toList();

        AuthResponse response = new AuthResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                roles,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        AuthResponse response =
                googleAuthService.loginWithGoogle(request.credential());

        return ResponseEntity.ok(response);
    }
}