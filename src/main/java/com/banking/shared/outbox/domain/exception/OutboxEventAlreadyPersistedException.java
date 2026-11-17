package com.banking.shared.outbox.domain.exception;

public class OutboxEventAlreadyPersistedException extends RuntimeException {

    public OutboxEventAlreadyPersistedException(String message) {
        super(message);
    }
}
