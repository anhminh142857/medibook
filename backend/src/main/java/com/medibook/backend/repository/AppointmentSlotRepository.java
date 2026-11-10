package com.medibook.backend.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.medibook.backend.entity.AppointmentSlot;

import jakarta.persistence.LockModeType;

public interface AppointmentSlotRepository
        extends JpaRepository<AppointmentSlot, Long> {

    List<AppointmentSlot> findByDoctorIdAndSlotDate(
            Long doctorId,
            LocalDate slotDate
    );

    List<AppointmentSlot> findByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select slot
            from AppointmentSlot slot
            where slot.id = :slotId
            """)
    Optional<AppointmentSlot> findByIdForUpdate(
            @Param("slotId") Long slotId
    );

    @Query("""
            select count(slot)
            from AppointmentSlot slot
            where slot.doctor.id = :doctorId
              and slot.slotDate = :slotDate
              and slot.startTime < :endTime
              and slot.endTime > :startTime
            """)
    long countOverlappingSlots(
            @Param("doctorId") Long doctorId,
            @Param("slotDate") LocalDate slotDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}