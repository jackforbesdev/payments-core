package com.jackforbes.paymentscore.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record AuthorisePaymentRequest(

    @Positive long amount,

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be in ISO 4217")
    String currency

) {}