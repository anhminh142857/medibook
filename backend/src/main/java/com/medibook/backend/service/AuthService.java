package com.medibook.backend.service;

import com.medibook.backend.dto.AuthResponse;
import com.medibook.backend.dto.LoginRequest;
import com.medibook.backend.dto.RegisterRequest;
import com.medibook.backend.entity.Role;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.RoleRepository;
import com.medibook.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public User registerPatient(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email da duoc su dung. Vui long su dung email khac."
            );
        }

        Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_PATIENT chua duoc khoi tao."
                ));

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setPhone(request.getPhone());
        user.setProvider("LOCAL");
        user.setStatus("ACTIVE");
        user.getRoles().add(patientRole);

        User savedUser = userRepository.save(user);

        log.info(
                "Da dang ky tai khoan benh nhan: {}",
                savedUser.getEmail()
        );

        return savedUser;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Email hoac mat khau khong dung."
                ));

        if (user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException(
                    "Tai khoan nay khong su dung mat khau. Vui long dang nhap bang Google."
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Email hoac mat khau khong dung."
            );
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        log.info("Dang nhap thanh cong: {}", user.getEmail());

        return new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roles,
                token
        );
    }
}