package com.medibook.backend.dto;

public class DoctorResponse {

    private Long id;

    private String fullName;

    private String phone;

    private String bio;

    private Long specialtyId;

    private String specialtyName;

    public DoctorResponse() {
    }

    public DoctorResponse(
            Long id,
            String fullName,
            String phone,
            String bio,
            Long specialtyId,
            String specialtyName
    ) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.bio = bio;
        this.specialtyId = specialtyId;
        this.specialtyName = specialtyName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSpecialtyName() {
        return specialtyName;
    }

    public void setSpecialtyName(String specialtyName) {
        this.specialtyName = specialtyName;
    }
}