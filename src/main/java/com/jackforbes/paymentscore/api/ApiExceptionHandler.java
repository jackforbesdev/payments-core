package com.jackforbes.paymentscore.api;

import com.jackforbes.paymentscore.service.IdempotencyMismatchException;
import com.jackforbes.paymentscore.service.InvalidInputException;
import com.jackforbes.paymentscore.service.InvalidTransitionException;
import com.jackforbes.paymentscore.service.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handleNotFound(PaymentNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not Found");
        pd.setDetail(ex.getMessage());
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidTransitionException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Invalid state transition");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", "INVALID_TRANSITION");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(InvalidInputException.class)
    public ProblemDetail handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid input");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", "INVALID_INPUT");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setProperty("code", "VALIDATION_ERROR");
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList());
        return pd;
    }

    @ExceptionHandler(IdempotencyMismatchException.class)
    public ProblemDetail handleIdemMismatch(IdempotencyMismatchException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Idempotency key reused");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", "IDEMPOTENCY_KEY_REUSED");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Concurrent update");
        pd.setDetail("The payment was modified by another request. Please retry.");
        pd.setProperty("code", "CONCURRENT_MODIFICATION");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

}
