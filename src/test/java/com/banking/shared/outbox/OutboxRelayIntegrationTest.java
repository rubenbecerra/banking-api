package com.banking.shared.outbox;

import com.banking.customer.AbstractTestContainers;
import com.banking.shared.outbox.application.OutboxRelay;
import com.banking.shared.outbox.domain.model.OutboxAggregateTypes;
import com.banking.shared.outbox.domain.model.OutboxEvent;
import com.banking.shared.outbox.domain.model.OutboxEventTypes;
import com.banking.shared.outbox.domain.repository.OutboxEventRepository;
import com.banking.shared.outbox.infrastructure.persistence.OutboxEventEntity;
import com.banking.shared.outbox.infrastructure.persistence.SpringDataOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayIntegrationTest extends AbstractTestContainers {

    private static final Duration LEASE = Duration.ofSeconds(30);
    private static final Instant T0 = Instant.parse("2026-11-18T10:00:00Z");

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private SpringDataOutboxEventRepository rows;

    private RecordingOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        rows.deleteAll();
        publisher = new RecordingOutboxPublisher();
    }

    @Test
    @DisplayName("a pending event is published once and marked")
    void publishesAndMarks() {
        OutboxEvent event = seedEvent(T0);

        int published = relayAt(T0).relayOnce(10);

        assertThat(published).isEqualTo(1);
        assertThat(publisher.publishedEvents())
                .extracting(OutboxEvent::id)
                .containsExactly(event.id());

        OutboxEventEntity stored = rows.findById(event.id()).orElseThrow();
        assertThat(stored.getClaimedAt()).isNotNull();
        assertThat(stored.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("a failed publish leaves the event pending and unmarked")
    void failedPublishLeavesEventPending() {
        OutboxEvent event = seedEvent(T0);
        publisher.failFor(event.id());

        int published = relayAt(T0).relayOnce(10);

        assertThat(published).isZero();
        assertThat(publisher.publishCount()).isZero();

        OutboxEventEntity stored = rows.findById(event.id()).orElseThrow();
        assertThat(stored.getClaimedAt()).isNotNull();
        assertThat(stored.getPublishedAt()).isNull();

        // Still inside the lease: another relay must not pick it up yet.
        assertThat(repository.findPending(10, now(T0))).isEmpty();
    }

    @Test
    @DisplayName("once the lease expires the relay retries and publishes")
    void retriesAfterLeaseExpires() {
        OutboxEvent event = seedEvent(T0);
        publisher.failFor(event.id());
        relayAt(T0).relayOnce(10);

        publisher.stopFailing();
        int published = relayAt(T0.plusSeconds(31)).relayOnce(10);

        assertThat(published).isEqualTo(1);
        assertThat(publisher.publishedEvents())
                .extracting(OutboxEvent::id)
                .containsExactly(event.id());
        assertThat(rows.findById(event.id()).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("an event already claimed by another relay is not published")
    void doesNotPublishAnEventClaimedByAnotherRelay() {
        OutboxEvent event = seedEvent(T0);
        assertThat(repository.claim(event.id(), now(T0), now(T0).minus(LEASE))).isTrue();

        int published = relayAt(T0).relayOnce(10);

        assertThat(published).isZero();
        assertThat(publisher.publishCount()).isZero();
    }

    @Test
    @DisplayName("delivery is at-least-once: a lost ACK republishes the same event")
    void lostAckRepublishesTheSameEvent() {
        OutboxEvent event = seedEvent(T0);
        publisher.deliverThenFailFor(event.id());

        // The broker stored it, but the relay could not mark it as published.
        int firstPass = relayAt(T0).relayOnce(10);

        assertThat(firstPass).isZero();
        assertThat(rows.findById(event.id()).orElseThrow().getPublishedAt()).isNull();

        // The ACK loss is transient: the next pass publishes it again.
        publisher.stopFailing();
        int secondPass = relayAt(T0.plusSeconds(31)).relayOnce(10);

        assertThat(secondPass).isEqualTo(1);
        // The same id travels twice on purpose. Consumers deduplicate by
        // outbox id; the relay cannot promise exactly-once delivery.
        assertThat(publisher.publishedEvents())
                .extracting(OutboxEvent::id)
                .containsExactly(event.id(), event.id());
    }

    private OutboxRelay relayAt(Instant instant) {
        return new OutboxRelay(repository, publisher,
                Clock.fixed(instant, ZoneId.systemDefault()), LEASE);
    }

    private OutboxEvent seedEvent(Instant instant) {
        return repository.append(OutboxEvent.pending(
                OutboxAggregateTypes.MOVEMENT,
                42L,
                OutboxEventTypes.MONEY_TRANSFERRED,
                "{\"movementId\":42,\"amount\":10.00}",
                now(instant)));
    }

    private static LocalDateTime now(Instant instant) {
        return LocalDateTime.now(Clock.fixed(instant, ZoneId.systemDefault()));
    }
}
