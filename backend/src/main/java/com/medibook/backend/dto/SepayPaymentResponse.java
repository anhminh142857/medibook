package com.medibook.backend.dto;

import java.time.LocalDateTime;

public record SepayPaymentResponse(
        Long paymentId,
        Long appointmentId,
        String txnRef,
        Long amount,
        String currency,
        String provider,
        String status,
        String transferContent,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        boolean requiresReview,
        String reviewReason,
        String receiverAccount,
        String receiverGateway
) {
}