package com.jackforbes.paymentscore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private long amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "payment_state")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentState state;

    @Column(name = "captured_amount", nullable = false)
    private long capturedAmount;

    @Column(name = "refunded_amount", nullable = false)
    private long refundedAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected Payment() {

    }

    // Getters
    public UUID getId() { return id; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentState getState() { return state; }
    public long getCapturedAmount() { return capturedAmount; }
    public long getRefundedAmount() { return refundedAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    // creates an authorised payment
    public static Payment authorised(UUID id, long amount, String currency, Instant now) {
        Payment payment = new Payment();
        payment.id = id;
        payment.amount = amount;
        payment.currency = currency;
        payment.state = PaymentState.AUTHORISED;
        payment.capturedAmount = 0;
        payment.refundedAmount = 0;
        payment.createdAt = now;
        payment.updatedAt = now;
        payment.version = 0;
        return payment;
    }

    public void capture(long amount, Instant now) {
        this.capturedAmount += amount;
        this.updatedAt = now;
        if(this.capturedAmount == this.amount) this.state = PaymentState.CAPTURED;
        else this.state = PaymentState.PARTIALLY_CAPTURED;

    }
}
