package com.banking.shared.outbox.application;

import com.banking.shared.outbox.domain.model.OutboxEvent;
import com.banking.shared.outbox.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final OutboxPublisher publisher;
    private final Clock clock;
    private final Duration lease;

    public OutboxRelay(OutboxEventRepository repository, OutboxPublisher publisher, Clock clock,
                       Duration lease) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
        this.lease = lease;
    }

    public int relayOnce(int batchSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(lease);
        List<OutboxEvent> pending = repository.findPending(batchSize, cutoff);
        int published = 0;
        for (OutboxEvent event : pending) {
            if (!repository.claim(event.id(), now, cutoff)) {
                continue;
            }
            try {
                publisher.publish(event);
            } catch (RuntimeException ex) {
                log.warn("Cannot publish outbox event {}", event.id(), ex);
                continue;
            }
            repository.markPublished(event.id(), LocalDateTime.now(clock));
            published++;
        }
        return published;
    }
}
