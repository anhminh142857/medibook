package com.medibook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SpecialtyRequest {

    @NotBlank(message = "Ten chuyen khoa khong duoc de trong")
    @Size(max = 100, message = "Ten chuyen khoa khong qua 100 ky tu")
    private String name;

    private String description;

    public SpecialtyRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}