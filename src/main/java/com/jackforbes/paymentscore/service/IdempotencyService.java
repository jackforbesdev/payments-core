package com.jackforbes.paymentscore.service;

import com.jackforbes.paymentscore.entity.IdempotencyKeyId;
import com.jackforbes.paymentscore.entity.IdempotencyKeyRecord;
import com.jackforbes.paymentscore.repo.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;


@Service
public class IdempotencyService {

    public record Replay(int status, UUID paymentId) {}

    private final IdempotencyKeyRepository repo;

    public IdempotencyService(IdempotencyKeyRepository repo) {
        this.repo = repo;
    }

    public String hash(String canonicalRequest) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash request", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Replay> checkReplayOrThrow(String clientId, String idemKey, String requestHash) {
        var id = new IdempotencyKeyId(clientId, idemKey);

        return repo.findById(id).map(existing -> {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyMismatchException(
                        "clientId=" + clientId + " reused Idempotency-Key with different request"
                );
            }
            return new Replay(existing.getResponseStatus(), existing.getPaymentId());
        });
    }

    @Transactional
    public void storeSuccess(
            String clientId,
            String idemKey,
            String requestHash,
            int responseStatus,
            UUID paymentId,
            Instant now
    ) {
        var id = new IdempotencyKeyId(clientId, idemKey);

        var record = new IdempotencyKeyRecord(
                id,
                requestHash,
                responseStatus,
                now,
                paymentId
        );

        repo.save(record);
    }

}