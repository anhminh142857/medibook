package com.medibook.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.backend.entity.Role;
import com.medibook.backend.repository.RoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createRoleIfNotExists("ROLE_PATIENT");
        createRoleIfNotExists("ROLE_DOCTOR");
        createRoleIfNotExists("ROLE_ADMIN");

        log.info("Khoi tao vai tro hoan tat.");
    }

    private void createRoleIfNotExists(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role(roleName);
            roleRepository.save(role);

            log.info("Da tao vai tro: {}", roleName);
        }
    }
}