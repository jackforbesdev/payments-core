package com.jackforbes.paymentscore.service;

import java.util.UUID;

public record StoredResponse(int status, UUID paymentId) {}
