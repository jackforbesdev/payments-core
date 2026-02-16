package com.jackforbes.paymentscore.service;


public record CaptureResult(boolean replayed, int status, String bodyJson) {
    public static CaptureResult replay(int status, String bodyJson) {
        return new CaptureResult(true, status, bodyJson);
    }
    public static CaptureResult fresh(int status, String bodyJson) {
        return new CaptureResult(false, status, bodyJson);
    }
}
