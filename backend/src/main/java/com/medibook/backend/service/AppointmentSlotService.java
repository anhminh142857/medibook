package com.medibook.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.dto.AppointmentSlotRequest;
import com.medibook.backend.dto.AppointmentSlotResponse;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.entity.Doctor;
import com.medibook.backend.repository.AppointmentSlotRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class AppointmentSlotService {

    private static final Logger log =
            LoggerFactory.getLogger(AppointmentSlotService.class);

    private final AppointmentSlotRepository appointmentSlotRepository;
    private final EntityManager entityManager;

    public AppointmentSlotService(
            AppointmentSlotRepository appointmentSlotRepository,
            EntityManager entityManager
    ) {
        this.appointmentSlotRepository = appointmentSlotRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlotResponse> getAllSlots() {
        return appointmentSlotRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentSlotResponse createSlot(
            AppointmentSlotRequest request
    ) {
        validateRequest(request);

        // Khoa ban ghi bac si de tuan tu hoa viec tao slot cung bac si.
        Doctor doctor = entityManager.find(
                Doctor.class,
                request.getDoctorId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        if (doctor == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Bác sĩ không tồn tại."
            );
        }

        LocalDateTime startDateTime = LocalDateTime.of(
                request.getSlotDate(),
                request.getStartTime()
        );

        if (!startDateTime.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời điểm bắt đầu khám phải ở tương lai."
            );
        }

        long overlappingCount =
                appointmentSlotRepository.countOverlappingSlots(
                        request.getDoctorId(),
                        request.getSlotDate(),
                        request.getStartTime(),
                        request.getEndTime()
                );

        if (overlappingCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bác sĩ đã có khung giờ chồng lấn trong ngày này."
            );
        }

        AppointmentSlot slot = new AppointmentSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(request.getSlotDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setStatus("AVAILABLE");

        AppointmentSlot savedSlot =
                appointmentSlotRepository.saveAndFlush(slot);

        log.info(
                "Da tao khung gio: id={}, doctorId={}, date={}, {} - {}",
                savedSlot.getId(),
                doctor.getId(),
                savedSlot.getSlotDate(),
                savedSlot.getStartTime(),
                savedSlot.getEndTime()
        );

        return toResponse(savedSlot);
    }

    private void validateRequest(AppointmentSlotRequest request) {
        if (request.getDoctorId() == null
                || request.getDoctorId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn bác sĩ hợp lệ."
            );
        }

        if (request.getSlotDate() == null
                || request.getStartTime() == null
                || request.getEndTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập đầy đủ ngày và giờ khám."
            );
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giờ kết thúc phải sau giờ bắt đầu."
            );
        }
    }

    private AppointmentSlotResponse toResponse(AppointmentSlot slot) {
        return new AppointmentSlotResponse(
                slot.getId(),
                slot.getDoctor().getId(),
                slot.getDoctor().getFullName(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}