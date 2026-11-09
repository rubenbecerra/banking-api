package com.banking.shared.idempotency.domain.exception;

public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String message) {
        super(message);
    }
}
