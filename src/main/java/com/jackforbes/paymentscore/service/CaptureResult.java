package com.jackforbes.paymentscore.service;


import java.util.UUID;

public record CaptureResult(int status, UUID paymentId, boolean replayed) {
    public static CaptureResult fresh(int status, UUID paymentId) {
        return new CaptureResult(status, paymentId, false);
    }
    public static CaptureResult replay(int status, UUID paymentId) {
        return new CaptureResult(status, paymentId, true);
    }
}
