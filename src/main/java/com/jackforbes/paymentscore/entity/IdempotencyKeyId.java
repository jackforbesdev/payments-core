package com.jackforbes.paymentscore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class IdempotencyKeyId implements Serializable {

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "idem_key", nullable = false)
    private String idemKey;

    protected IdempotencyKeyId() {}

    public IdempotencyKeyId(String clientId, String idemKey) {
        this.clientId = clientId;
        this.idemKey = idemKey;
    }

    public String getClientId() { return clientId; }
    public String getIdemKey() { return idemKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyKeyId that = (IdempotencyKeyId) o;
        return Objects.equals(clientId, that.clientId)
                && Objects.equals(idemKey, that.idemKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, idemKey);
    }
}
