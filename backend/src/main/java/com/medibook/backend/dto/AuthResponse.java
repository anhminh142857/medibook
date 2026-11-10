package com.medibook.backend.dto;

import java.util.List;

public class AuthResponse {

    private Long id;

    private String fullName;

    private String email;

    private String phone;

    private List<String> roles;

    private String token;

    public AuthResponse() {
    }

    public AuthResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            List<String> roles,
            String token
    ) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.roles = roles;
        this.token = token;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}