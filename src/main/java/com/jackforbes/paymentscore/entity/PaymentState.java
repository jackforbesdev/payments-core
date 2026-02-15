package com.jackforbes.paymentscore.entity;

public enum PaymentState {
    AUTHORISED,
    PARTIALLY_CAPTURED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    VOIDED
}
