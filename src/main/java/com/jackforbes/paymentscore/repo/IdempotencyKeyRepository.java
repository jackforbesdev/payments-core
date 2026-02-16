package com.jackforbes.paymentscore.repo;

import com.jackforbes.paymentscore.entity.IdempotencyKeyId;
import com.jackforbes.paymentscore.entity.IdempotencyKeyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, IdempotencyKeyId> {
}
