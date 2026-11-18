package com.banking.shared.outbox.application;

import com.banking.shared.outbox.domain.model.OutboxEvent;

public interface OutboxPublisher {
    void publish (OutboxEvent event);
}
