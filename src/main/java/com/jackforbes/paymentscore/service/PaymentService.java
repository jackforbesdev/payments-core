package com.jackforbes.paymentscore.service;

import com.jackforbes.paymentscore.entity.Payment;
import com.jackforbes.paymentscore.repo.PaymentRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Clock clock;

    public PaymentService(PaymentRepository paymentRepository, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    @Transactional
    public Payment authorise(long amount, String currency){
        Instant now = Instant.now(clock);
        Payment payment = Payment.authorised(UUID.randomUUID(), amount, currency, now);
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getById(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
