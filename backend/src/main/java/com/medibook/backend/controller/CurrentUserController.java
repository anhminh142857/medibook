package com.medibook.backend.controller;

import com.medibook.backend.dto.AuthResponse;
import com.medibook.backend.entity.Role;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class CurrentUserController {

    private final UserRepository userRepository;

    public CurrentUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(
            Principal principal
    ) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Tai khoan khong ton tai."
                ));

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList(),
                null
        );

        return ResponseEntity.ok(response);
    }
}