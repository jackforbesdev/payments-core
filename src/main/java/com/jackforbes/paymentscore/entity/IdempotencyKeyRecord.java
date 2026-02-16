package com.jackforbes.paymentscore.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyRecord {

    @EmbeddedId
    private IdempotencyKeyId id;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyKeyRecord() {}

    public IdempotencyKeyRecord(
            IdempotencyKeyId id,
            String requestHash,
            int responseStatus,
            Instant createdAt,
            UUID paymentId
    ) {
        this.id = id;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.createdAt = createdAt;
        this.paymentId = paymentId;
    }

    public IdempotencyKeyId getId() { return id; }
    public String getRequestHash() { return requestHash; }
    public int getResponseStatus() { return responseStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getPaymentId() { return paymentId; }
}
