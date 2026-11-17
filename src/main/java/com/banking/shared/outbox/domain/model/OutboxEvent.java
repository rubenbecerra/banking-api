package com.banking.shared.outbox.domain.model;

import java.time.LocalDateTime;

/**
 * One row of the outbox table.
 *
 * <p>This is intentionally not a pure business event: {@code claimedAt} and
 * {@code publishedAt} are relay bookkeeping, not facts produced by the ledger.
 * Keeping a single type avoids duplicating a record that, in v1, has exactly one
 * producer; if the relay ever grows its own state machine, a separate
 * infrastructure-level {@code PendingEvent} would be the cleaner split.
 *
 * <p>Invariants are enforced twice on purpose: here (fail fast, with a readable
 * message) and in the V7 schema (the database is the last line of defence).
 * A record is public API: the canonical constructor is a door too, so it
 * validates as well.
 *
 * <p>There is no method to mark an event as claimed or published: that is done
 * with conditional SQL against the row, never by loading and saving the entity.
 */
public record OutboxEvent(
        Long id,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String payload,
        LocalDateTime createdAt,
        LocalDateTime claimedAt,
        LocalDateTime publishedAt
) {

    private static final int AGGREGATE_TYPE_MAX_LENGTH = 32;
    private static final int EVENT_TYPE_MAX_LENGTH = 60;

    public OutboxEvent {
        requireText(aggregateType, "aggregateType", AGGREGATE_TYPE_MAX_LENGTH);
        if (aggregateId == null || aggregateId <= 0) {
            throw new IllegalArgumentException("aggregateId must be positive");
        }
        requireText(eventType, "eventType", EVENT_TYPE_MAX_LENGTH);
        requireText(payload, "payload", Integer.MAX_VALUE);
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        // Mirrors chk_outbox_published_after_claim in V7.
        if (publishedAt != null && claimedAt == null) {
            throw new IllegalArgumentException("publishedAt requires claimedAt");
        }
    }

    /**
     * A new, unpublished event. It cannot be built already published: that is
     * the whole point of the factory.
     */
    public static OutboxEvent pending(String aggregateType,
                                      long aggregateId,
                                      String eventType,
                                      String payload,
                                      LocalDateTime createdAt) {
        return new OutboxEvent(null,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                createdAt,
                null,
                null);
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must be at most " + maxLength + " characters");
        }
    }
}
