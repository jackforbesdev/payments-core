package com.jackforbes.paymentscore.api;

/*
DTO to take some/all of the authorised money of a payment
 */

import jakarta.validation.constraints.Min;

public record CapturePaymentRequest (
        @Min(1) long amount
) {}
