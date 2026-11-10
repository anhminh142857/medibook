package com.medibook.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibook.backend.entity.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserEmail(String email);

    boolean existsByUserIdAndIdNot(Long userId, Long doctorId);
}