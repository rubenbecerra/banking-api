package com.banking.shared.outbox.infrastructure.publisher;

import com.banking.shared.outbox.application.OutboxPublisher;
import com.banking.shared.outbox.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutboxPublisher implements OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxPublisher.class);

    @Override
    public void publish(OutboxEvent event) {
        log.info("Published outbox event {} type={} aggregate={}",
                event.id(), event.eventType(), event.aggregateId());
        log.debug("Payload: {}", event.payload());
    }
}
