package com.jackforbes.paymentscore.api;

import com.jackforbes.paymentscore.entity.PaymentState;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse (
        UUID id,
        long amount,
        String currency,
        PaymentState state,
        long capturedAmount,
        long refundedAmount,
        Instant createdAt,
        Instant updatedAt
) {}
