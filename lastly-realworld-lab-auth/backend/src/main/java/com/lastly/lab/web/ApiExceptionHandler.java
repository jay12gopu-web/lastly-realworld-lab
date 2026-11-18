package com.lastly.lab.web;

import com.lastly.lab.service.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(String message) {}

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> missing(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> business(DomainException ex) {
        HttpStatus status = ex.getMessage().startsWith("Order creation disabled")
            ? HttpStatus.FORBIDDEN : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(DataIntegrityViolationException ignored) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError("Concurrent update or duplicate request. Retry with the same idempotency key."));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiError> authentication(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(
            ex.getStatusCode().value()==403 ? "Authentication disabled in this environment" : "Invalid email or password"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ignored) {
        return ResponseEntity.badRequest().body(new ApiError("Invalid order: productId, quantity (1-50), and idempotencyKey are required"));
    }
}
