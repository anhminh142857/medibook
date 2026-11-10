package com.medibook.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.dto.DoctorRequest;
import com.medibook.backend.dto.DoctorResponse;
import com.medibook.backend.entity.Doctor;
import com.medibook.backend.entity.Specialty;
import com.medibook.backend.repository.DoctorRepository;
import com.medibook.backend.repository.SpecialtyRepository;

@Service
public class DoctorService {

    private static final Logger log =
            LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;

    public DoctorService(
            DoctorRepository doctorRepository,
            SpecialtyRepository specialtyRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        Specialty specialty = findSpecialty(request.getSpecialtyId());

        Doctor doctor = new Doctor();
        applyRequest(doctor, request, specialty);

        Doctor savedDoctor = doctorRepository.save(doctor);

        log.info(
                "Da tao bac si: id={}, fullName={}",
                savedDoctor.getId(),
                savedDoctor.getFullName()
        );

        return toResponse(savedDoctor);
    }

    @Transactional
    public DoctorResponse updateDoctor(
            Long id,
            DoctorRequest request
    ) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bác sĩ có ID " + id + "."
                ));

        Specialty specialty = findSpecialty(request.getSpecialtyId());

        applyRequest(doctor, request, specialty);

        Doctor savedDoctor = doctorRepository.save(doctor);

        log.info(
                "Da cap nhat bac si: id={}, fullName={}",
                savedDoctor.getId(),
                savedDoctor.getFullName()
        );

        return toResponse(savedDoctor);
    }

    private Specialty findSpecialty(Long specialtyId) {
        return specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Chuyên khoa được chọn không tồn tại."
                ));
    }

    private void applyRequest(
            Doctor doctor,
            DoctorRequest request,
            Specialty specialty
    ) {
        doctor.setFullName(request.getFullName().trim());
        doctor.setPhone(trimNullable(request.getPhone()));
        doctor.setBio(trimNullable(request.getBio()));
        doctor.setSpecialty(specialty);
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getPhone(),
                doctor.getBio(),
                doctor.getSpecialty().getId(),
                doctor.getSpecialty().getName()
        );
    }
}