package com.medibook.backend.config;

import com.medibook.backend.entity.Role;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.RoleRepository;
import com.medibook.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${MEDIBOOK_ADMIN_EMAIL:}") String adminEmail,
            @Value("${MEDIBOOK_ADMIN_PASSWORD:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            log.info(
                    "Bo qua khoi tao admin: email {} da ton tai. "
                            + "Khong thay doi mat khau hoac vai tro.",
                    email
            );
            return;
        }

        int passwordBytes = adminPassword
                .getBytes(StandardCharsets.UTF_8)
                .length;

        if (adminPassword.length() < 12 || passwordBytes > 72) {
            throw new IllegalArgumentException(
                    "Mat khau admin phai co it nhat 12 ky tu "
                            + "va khong qua 72 byte UTF-8."
            );
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() ->
                        roleRepository.save(new Role("ROLE_ADMIN"))
                );

        User admin = new User();
        admin.setFullName("Quan tri vien MediBook");
        admin.setEmail(email);
        admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
        );
        admin.setProvider("LOCAL");
        admin.setStatus("ACTIVE");
        admin.getRoles().add(adminRole);

        userRepository.save(admin);

        log.info("Da khoi tao tai khoan admin: {}", email);
    }
}