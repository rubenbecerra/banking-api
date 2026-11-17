package com.banking.shared.outbox.domain.repository;

import com.banking.shared.outbox.domain.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox persistence.
 *
 * The event is saved in the same transaction as the business change. A relay
 * publishes it later, so the same event can arrive twice: consumers deduplicate
 * by id.
 *
 * The lease policy (how long a claim lasts) is not here: the caller passes the
 * cutoff, so the adapter can be tested with fixed instants.
 */
public interface OutboxEventRepository {

    /**
     * Saves a new event in the caller's transaction and returns it with its id.
     *
     * @throws OutboxEventAlreadyPersistedException if the event already has an id
     */
    OutboxEvent append(OutboxEvent event);

    /**
     * Pending events, oldest first: not published, and not claimed by another
     * relay within the lease. The caller computes {@code claimCutoff} as
     * {@code now - lease}.
     *
     * @throws IllegalArgumentException if {@code limit} is not positive
     */
    List<OutboxEvent> findPending(int limit, LocalDateTime claimCutoff);

    /**
     * Tries to claim one event. Returns {@code false} if another relay got it
     * first or it is already published. Uses the same cutoff rule as
     * {@link #findPending(int, LocalDateTime)}.
     */
    boolean claim(long id, LocalDateTime claimedAt, LocalDateTime staleCutoff);

    /**
     * Marks an event as published if it is still pending. Returns {@code false}
     * if another relay marked it first.
     */
    boolean markPublished(long id, LocalDateTime publishedAt);

    /** Deletes published events older than the cutoff. Returns the row count. */
    int deletePublishedBefore(LocalDateTime cutoff);
}
