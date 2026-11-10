package com.medibook.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.backend.dto.AuthResponse;
import com.medibook.backend.entity.Role;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.RoleRepository;
import com.medibook.backend.repository.UserRepository;

@Service
public class GoogleAuthService {

    private static final Logger log =
            LoggerFactory.getLogger(GoogleAuthService.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    public GoogleAuthService(
            GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtService jwtService
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse loginWithGoogle(String credential) {
        GoogleTokenVerifier.GoogleProfile profile =
                googleTokenVerifier.verify(credential);

        User user = userRepository.findByEmail(profile.email()).orElse(null);

        if (user == null) {
            user = createGoogleUser(profile);
        } else {
            validateExistingUser(user, profile);

            if (user.getProviderId() == null
                    || user.getProviderId().isBlank()) {
                user.setProviderId(profile.subject());
                user = userRepository.save(user);
            }
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        log.info("Dang nhap Google thanh cong: {}", user.getEmail());

        return new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roles,
                token
        );
    }

    private User createGoogleUser(GoogleTokenVerifier.GoogleProfile profile) {
        Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_PATIENT chua duoc khoi tao."
                ));

        String fullName = profile.fullName();

        if (fullName == null || fullName.isBlank()) {
            fullName = profile.email().substring(
                    0,
                    profile.email().indexOf('@')
            );
        }

        fullName = fullName.trim();

        if (fullName.length() > 100) {
            fullName = fullName.substring(0, 100);
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(profile.email());
        user.setPasswordHash(null);
        user.setProvider("GOOGLE");
        user.setProviderId(profile.subject());
        user.setStatus("ACTIVE");
        user.getRoles().add(patientRole);

        return userRepository.save(user);
    }

    private void validateExistingUser(
            User user,
            GoogleTokenVerifier.GoogleProfile profile
    ) {
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException(
                    "Tai khoan khong hoat dong."
            );
        }

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));

        if (isAdmin) {
            throw new IllegalArgumentException(
                    "Tai khoan quan tri khong ho tro dang nhap Google."
            );
        }

        String existingProviderId = user.getProviderId();

        if (existingProviderId != null
                && !existingProviderId.isBlank()
                && !existingProviderId.equals(profile.subject())) {
            throw new IllegalArgumentException(
                    "Tai khoan nay da lien ket voi mot tai khoan Google khac."
            );
        }
    }
}