package com.jackforbes.paymentscore.api;


import com.jackforbes.paymentscore.entity.Payment;
import com.jackforbes.paymentscore.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
