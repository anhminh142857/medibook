package com.medibook.backend.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "payment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_txn_ref",
                        columnNames = "txn_ref"
                )
        },
        indexes = {
                @Index(
                        name = "idx_payment_appointment_status",
                        columnList = "appointment_id,status"
                )
        }
)
@Getter
@Setter
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "txn_ref", nullable = false, length = 100)
    private String txnRef;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(nullable = false, length = 20)
    private String provider = "VNPAY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "gateway_transaction_no", length = 100)
    private String gatewayTransactionNo;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "gateway_transaction_status", length = 10)
    private String gatewayTransactionStatus;

    @Column(name = "payment_url", length = 4096)
    private String paymentUrl;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "requires_review", nullable = false)
    private boolean requiresReview = false;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaymentTransaction() {
    }

    @PrePersist
    public void beforeCreate() {
        LocalDateTime now = LocalDateTime.now(
                ZoneId.of("Asia/Ho_Chi_Minh")
        );

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        validateRequiredFields();
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now(
                ZoneId.of("Asia/Ho_Chi_Minh")
        );

        validateRequiredFields();
    }

    private void validateRequiredFields() {
        if (appointment == null) {
            throw new IllegalStateException(
                    "Giao dich phai gan voi mot lich hen."
            );
        }

        if (txnRef == null || txnRef.isBlank()) {
            throw new IllegalStateException(
                    "Giao dich phai co ma tham chieu."
            );
        }

        if (amount == null || amount <= 0) {
            throw new IllegalStateException(
                    "So tien giao dich phai lon hon 0."
            );
        }

        if (expiresAt == null) {
            throw new IllegalStateException(
                    "Giao dich phai co thoi diem het han."
            );
        }

        if (status == null) {
            throw new IllegalStateException(
                    "Giao dich phai co trang thai."
            );
        }

        if (!"VND".equals(currency)) {
            throw new IllegalStateException(
                    "Chi ho tro don vi tien te VND."
            );
        }

        if (!"VNPAY".equals(provider)) {
            throw new IllegalStateException(
                    "Chi ho tro cong thanh toan VNPAY."
            );
        }
    }
}