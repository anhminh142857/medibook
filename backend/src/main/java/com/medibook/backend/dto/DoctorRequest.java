package com.medibook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class DoctorRequest {

    @NotBlank(message = "Họ tên bác sĩ không được để trống")
    @Size(
            max = 100,
            message = "Họ tên bác sĩ không quá 100 ký tự"
    )
    private String fullName;

    @Size(
            max = 20,
            message = "Số điện thoại không quá 20 ký tự"
    )
    private String phone;

    @Size(
            max = 255,
            message = "Giới thiệu bác sĩ không quá 255 ký tự"
    )
    private String bio;

    @NotNull(message = "Chuyên khoa không được để trống")
    @Positive(message = "ID chuyên khoa phải lớn hơn 0")
    private Long specialtyId;

    public DoctorRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Long getSpecialtyId() {
        return specialtyId;
    }

    public void setSpecialtyId(Long specialtyId) {
        this.specialtyId = specialtyId;
    }
}