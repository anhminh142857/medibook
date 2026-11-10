package com.medibook.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.medibook.backend.entity.Appointment;

import jakarta.persistence.LockModeType;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select appointment
            from Appointment appointment
            where appointment.id = :appointmentId
            """)
    Optional<Appointment> findByIdForUpdate(
            @Param("appointmentId") Long appointmentId
    );

    @Query("""
            select appointment
            from Appointment appointment
            join fetch appointment.patient patient
            join fetch appointment.doctor
            join fetch appointment.slot
            where patient.email = :email
            order by appointment.createdAt desc, appointment.id desc
            """)
    List<Appointment> findAllForPatientEmail(
            @Param("email") String email
    );

    @Query("""
            select appointment
            from Appointment appointment
            join fetch appointment.patient
            join fetch appointment.doctor doctor
            join fetch appointment.slot slot
            where doctor.id = :doctorId
            order by slot.slotDate asc,
                     slot.startTime asc,
                     appointment.id desc
            """)
    List<Appointment> findAllForDoctorId(
            @Param("doctorId") Long doctorId
    );
}