package com.banking.shared.outbox;

import com.banking.shared.outbox.application.OutboxPublisher;
import com.banking.shared.outbox.domain.model.OutboxEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test double for the broker.
 *
 * Records what was published and can be told to fail, either for one event id
 * or for everything, so the relay's retry behaviour can be exercised without a
 * real broker. {@link #deliverThenFailFor(long)} models the worst case: the
 * broker DID accept the message, but the relay never learns it (lost ACK).
 */
class RecordingOutboxPublisher implements OutboxPublisher {

    private final List<OutboxEvent> published = new CopyOnWriteArrayList<>();
    private final Set<Long> failingIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> ackLostIds = ConcurrentHashMap.newKeySet();
    private volatile boolean failEverything;

    @Override
    public void publish(OutboxEvent event) {
        if (failEverything || failingIds.contains(event.id())) {
            throw new IllegalStateException("Broker unavailable");
        }
        published.add(event);
        if (ackLostIds.contains(event.id())) {
            throw new IllegalStateException("ACK lost after delivery");
        }
    }

    void failFor(long eventId) {
        failingIds.add(eventId);
    }

    /** The broker stores the event and then the acknowledgement is lost. */
    void deliverThenFailFor(long eventId) {
        ackLostIds.add(eventId);
    }

    void stopFailing() {
        failingIds.clear();
        ackLostIds.clear();
        failEverything = false;
    }

    void failEverything(boolean value) {
        this.failEverything = value;
    }

    void reset() {
        published.clear();
        stopFailing();
    }

    List<OutboxEvent> publishedEvents() {
        return List.copyOf(published);
    }

    int publishCount() {
        return published.size();
    }
}
