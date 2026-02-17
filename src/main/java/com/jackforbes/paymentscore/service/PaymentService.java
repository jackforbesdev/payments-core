package com.jackforbes.paymentscore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackforbes.paymentscore.api.PaymentResponse;
import com.jackforbes.paymentscore.entity.Payment;
import com.jackforbes.paymentscore.entity.PaymentState;
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
    private final IdempotencyService idempotencyService;

    public PaymentService(PaymentRepository paymentRepository, Clock clock, IdempotencyService idempotencyService) {
        this.paymentRepository = paymentRepository;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
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

    @Transactional
    public CaptureResult capture(UUID id, String clientId, String idempotencyKey, long captureAmount) {
        Instant now = Instant.now(clock);

        if (captureAmount <= 0) {
            throw new InvalidInputException("captureAmount must be > 0");
        }

        String canonical = "CAPTURE|paymentId=" + id + "|amount=" + captureAmount;
        String hash = idempotencyService.hash(canonical);

        var replay = idempotencyService.checkReplayOrThrow(clientId, idempotencyKey, hash);
        if (replay.isPresent()) {
            return CaptureResult.replay(replay.get().status(), replay.get().paymentId());
        }

        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getState() != PaymentState.AUTHORISED &&
                payment.getState() != PaymentState.PARTIALLY_CAPTURED) {
            throw new InvalidTransitionException("Illegal capture in state " + payment.getState());
        }

        long newCaptured = payment.getCapturedAmount() + captureAmount;
        if (newCaptured > payment.getAmount()) {
            throw new InvalidTransitionException("Capture would exceed authorised amount");
        }

        payment.capture(captureAmount, now);
        paymentRepository.save(payment);

        idempotencyService.storeSuccess(clientId, idempotencyKey, hash, 200, payment.getId(), now);
        return CaptureResult.fresh(200, payment.getId());
    }


    @Transactional
    public RefundResult refund(UUID id, String clientId, String idempotencyKey, long refundAmount) {
        Instant now = Instant.now(clock);

        if (refundAmount <= 0) {
            throw new InvalidInputException("refundAmount must be > 0");
        }

        String canonical = "REFUND|paymentId=" + id + "|amount=" + refundAmount;
        String hash = idempotencyService.hash(canonical);

        var replay = idempotencyService.checkReplayOrThrow(clientId, idempotencyKey, hash);
        if (replay.isPresent()) {
            return RefundResult.replay(replay.get().status(), replay.get().paymentId());
        }

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getState() != PaymentState.CAPTURED &&
                payment.getState() != PaymentState.PARTIALLY_CAPTURED &&
                payment.getState() != PaymentState.PARTIALLY_REFUNDED) {
            throw new InvalidTransitionException("Illegal refund in state " + payment.getState());
        }

        long newRefunded = payment.getRefundedAmount() + refundAmount;

        if (newRefunded > payment.getCapturedAmount()) {
            throw new InvalidTransitionException("Refund would exceed captured amount");
        }

        payment.refund(refundAmount, now);
        paymentRepository.save(payment);

        idempotencyService.storeSuccess(clientId, idempotencyKey, hash, 200, payment.getId(), now);
        return RefundResult.fresh(200, payment.getId());
    }

}
