package com.medibook.backend.dto;

import jakarta.validation.constraints.NotNull;

public class AppointmentRequest {

    @NotNull(message = "Khung gio khong duoc de trong")
    private Long slotId;

    public AppointmentRequest() {
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }
}