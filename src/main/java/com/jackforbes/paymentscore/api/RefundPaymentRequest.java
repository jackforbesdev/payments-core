package com.jackforbes.paymentscore.api;

/*
DTO to refund some/all of the money from a payment
 */

import jakarta.validation.constraints.Min;

public record RefundPaymentRequest (
        @Min(1) long amount
){}
