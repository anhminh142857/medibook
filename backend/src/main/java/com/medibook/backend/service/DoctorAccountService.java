package com.medibook.backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.entity.Doctor;
import com.medibook.backend.entity.Role;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.DoctorRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class DoctorAccountService {

    private final DoctorRepository doctorRepository;
    private final EntityManager entityManager;

    public DoctorAccountService(
            DoctorRepository doctorRepository,
            EntityManager entityManager
    ) {
        this.doctorRepository = doctorRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public DoctorAccountResponse linkAccount(
            Long doctorId,
            String email
    ) {
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        List<User> users = entityManager.createQuery(
                        """
                        select u
                        from User u
                        where lower(u.email) = :email
                        """,
                        User.class
                )
                .setParameter("email", normalizedEmail)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        if (users.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Email chưa có tài khoản. Vui lòng đăng ký tài khoản trước."
            );
        }

        if (users.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Có nhiều tài khoản trùng email khi bỏ qua chữ hoa/thường."
            );
        }

        User user = users.get(0);

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tài khoản được chọn không hoạt động."
            );
        }

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role ->
                        "ROLE_ADMIN".equals(role.getName())
                );

        if (isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không dùng tài khoản admin làm tài khoản bác sĩ."
            );
        }

        Doctor doctor = entityManager.find(
                Doctor.class,
                doctorId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (doctor == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hồ sơ bác sĩ không tồn tại."
            );
        }

        if (doctor.getUser() != null
                && !doctor.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Hồ sơ bác sĩ đã liên kết với tài khoản khác."
            );
        }

        if (doctorRepository.existsByUserIdAndIdNot(
                user.getId(),
                doctor.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tài khoản đã liên kết với hồ sơ bác sĩ khác."
            );
        }

        Role doctorRole = entityManager.createQuery(
                        """
                        select r
                        from Role r
                        where r.name = :name
                        """,
                        Role.class
                )
                .setParameter("name", "ROLE_DOCTOR")
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Chưa có ROLE_DOCTOR. Cần kiểm tra khởi tạo vai trò."
                ));

        boolean alreadyHasRole = user.getRoles().stream()
                .anyMatch(role ->
                        "ROLE_DOCTOR".equals(role.getName())
                );

        if (!alreadyHasRole) {
            user.getRoles().add(doctorRole);
        }

        doctor.setUser(user);

        entityManager.flush();

        return new DoctorAccountResponse(
                doctor.getId(),
                doctor.getFullName(),
                user.getId(),
                user.getEmail(),
                "Đã liên kết tài khoản bác sĩ."
        );
    }

    public record DoctorAccountResponse(
            Long doctorId,
            String doctorName,
            Long userId,
            String email,
            String message
    ) {
    }
}