package com.medibook.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sepay_webhook_events")
@Getter
@Setter
public class SepayWebhookEvent {

    @Id
    @Column(name = "event_key", length = 80, nullable = false)
    private String eventKey;

    @Column(name = "payload_hash", length = 64, nullable = false)
    private String payloadHash;

    @Column(length = 30, nullable = false)
    private String outcome;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(length = 255, nullable = false)
    private String note;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}