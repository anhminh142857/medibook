package com.medibook.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentSlotRequest {

    @NotNull(message = "Bac si khong duoc de trong")
    private Long doctorId;

    @NotNull(message = "Ngay kham khong duoc de trong")
    private LocalDate slotDate;

    @NotNull(message = "Gio bat dau khong duoc de trong")
    private LocalTime startTime;

    @NotNull(message = "Gio ket thuc khong duoc de trong")
    private LocalTime endTime;

    public AppointmentSlotRequest() {
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}