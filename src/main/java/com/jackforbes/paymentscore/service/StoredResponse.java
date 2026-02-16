package com.jackforbes.paymentscore.service;

public record StoredResponse(
        int status,
        String bodyJson
) {}
