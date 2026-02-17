package com.jackforbes.paymentscore.service;

import java.util.UUID;

public record RefundResult(int status, UUID paymentId, boolean replayed) {

    public static RefundResult fresh(int status, UUID paymentId) {
        return new RefundResult(status, paymentId, false);
    }

    public static RefundResult replay(int status, UUID paymentId) {
        return new RefundResult(status, paymentId, true);
    }
}
