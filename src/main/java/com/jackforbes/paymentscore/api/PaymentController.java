package com.jackforbes.paymentscore.api;

import com.jackforbes.paymentscore.entity.Payment;
import com.jackforbes.paymentscore.service.CaptureResult;
import com.jackforbes.paymentscore.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorise")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse authorise(@Valid @RequestBody AuthorisePaymentRequest request) {
        Payment payment = paymentService.authorise(request.amount(), request.currency());
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getState(),
                payment.getCapturedAmount(),
                payment.getRefundedAmount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    @PostMapping("/{id}/capture")
    public PaymentResponse capture(
            @PathVariable UUID id,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CapturePaymentRequest request
    ) {
        Payment payment = paymentService.capture(id, clientId, idempotencyKey, request.amount());
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getState(),
                payment.getCapturedAmount(),
                payment.getRefundedAmount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(
            @PathVariable UUID id,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RefundPaymentRequest request
    ) {
        throw new UnsupportedOperationException();
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        Payment payment = paymentService.getById(id);
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getState(),
                payment.getCapturedAmount(),
                payment.getRefundedAmount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
