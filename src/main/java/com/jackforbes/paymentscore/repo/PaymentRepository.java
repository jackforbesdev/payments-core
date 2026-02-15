package com.jackforbes.paymentscore.repo;

import com.jackforbes.paymentscore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
