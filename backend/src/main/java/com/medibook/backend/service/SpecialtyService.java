package com.medibook.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.dto.SpecialtyRequest;
import com.medibook.backend.dto.SpecialtyResponse;
import com.medibook.backend.entity.Specialty;
import com.medibook.backend.repository.SpecialtyRepository;

@Service
public class SpecialtyService {

    private static final Logger log =
            LoggerFactory.getLogger(SpecialtyService.class);

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> getAllSpecialties() {
        return specialtyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SpecialtyResponse createSpecialty(SpecialtyRequest request) {
        Specialty specialty = new Specialty();
        specialty.setName(request.getName().trim());
        specialty.setDescription(request.getDescription());

        Specialty savedSpecialty = specialtyRepository.save(specialty);

        log.info(
                "Da tao chuyen khoa: id={}, name={}",
                savedSpecialty.getId(),
                savedSpecialty.getName()
        );

        return toResponse(savedSpecialty);
    }

    @Transactional
    public SpecialtyResponse updateSpecialty(
            Long id,
            SpecialtyRequest request
    ) {
        Specialty specialty = findSpecialty(id);

        specialty.setName(request.getName().trim());
        specialty.setDescription(request.getDescription());

        Specialty savedSpecialty = specialtyRepository.save(specialty);

        log.info(
                "Da cap nhat chuyen khoa: id={}, name={}",
                savedSpecialty.getId(),
                savedSpecialty.getName()
        );

        return toResponse(savedSpecialty);
    }

    @Transactional
    public void deleteSpecialty(Long id) {
        Specialty specialty = findSpecialty(id);

        try {
            specialtyRepository.delete(specialty);

            // Thuc thi SQL ngay de bat loi rang buoc trong phuong thuc nay.
            specialtyRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa chuyên khoa vì dữ liệu đang được "
                            + "tham chiếu hoặc vi phạm ràng buộc dữ liệu.",
                    exception
            );
        }

        log.info(
                "Da xoa chuyen khoa: id={}, name={}",
                id,
                specialty.getName()
        );
    }

    private Specialty findSpecialty(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy chuyên khoa có ID " + id + "."
                ));
    }

    private SpecialtyResponse toResponse(Specialty specialty) {
        return new SpecialtyResponse(
                specialty.getId(),
                specialty.getName(),
                specialty.getDescription()
        );
    }
}