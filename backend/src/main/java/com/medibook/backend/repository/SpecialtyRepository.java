package com.medibook.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibook.backend.entity.Specialty;

public interface SpecialtyRepository
        extends JpaRepository<Specialty, Long> {
}