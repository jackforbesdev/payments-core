package com.jackforbes.paymentscore.service;

public class IdempotencyMistmatchException extends RuntimeException {
    public IdempotencyMistmatchException(String message) {
        super(message);
    }
}
