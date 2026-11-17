package com.banking.shared.outbox;

import com.banking.customer.AbstractTestContainers;
import com.banking.shared.outbox.domain.exception.OutboxEventAlreadyPersistedException;
import com.banking.shared.outbox.domain.model.OutboxAggregateTypes;
import com.banking.shared.outbox.domain.model.OutboxEvent;
import com.banking.shared.outbox.domain.model.OutboxEventTypes;
import com.banking.shared.outbox.domain.repository.OutboxEventRepository;
import com.banking.shared.outbox.infrastructure.persistence.SpringDataOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxRepositoryIntegrationTest extends AbstractTestContainers {

    @Autowired
    private OutboxEventRepository outbox;

    @Autowired
    private SpringDataOutboxEventRepository rows;

    @BeforeEach
    void cleanTable() {
        rows.deleteAll();
    }

    @Test
    @DisplayName("append stores the event unpublished and returns the generated id")
    void appendReturnsStoredEvent() {
        LocalDateTime createdAt = now();

        OutboxEvent stored = outbox.append(pending(createdAt));

        assertThat(stored.id()).isNotNull();
        assertThat(stored.createdAt()).isEqualTo(createdAt);
        assertThat(stored.claimedAt()).isNull();
        assertThat(stored.publishedAt()).isNull();
        assertThat(stored.payload()).contains("42");
    }

    @Test
    @DisplayName("append rejects an event that already has an id")
    void appendRejectsPersistedEvent() {
        OutboxEvent alreadyStored = new OutboxEvent(1L, OutboxAggregateTypes.MOVEMENT, 42L,
                OutboxEventTypes.MONEY_TRANSFERRED, "{}", now(), null, null);

        assertThatThrownBy(() -> outbox.append(alreadyStored))
                .isInstanceOf(OutboxEventAlreadyPersistedException.class)
                .hasMessageContaining("already persisted");
    }

    @Test
    @DisplayName("findPending returns oldest first and honours the limit")
    void findPendingOrdersAndLimits() {
        OutboxEvent oldest = outbox.append(pending(now().minusSeconds(2)));
        OutboxEvent newest = outbox.append(pending(now()));

        List<OutboxEvent> pending = outbox.findPending(10, now());

        assertThat(pending).extracting(OutboxEvent::id)
                .containsExactly(oldest.id(), newest.id());
        assertThat(outbox.findPending(1, now()))
                .extracting(OutboxEvent::id)
                .containsExactly(oldest.id());
    }

    @Test
    @DisplayName("claim is exclusive while the lease is alive and can be taken again once it expires")
    void claimRespectsTheLease() {
        OutboxEvent event = outbox.append(pending(now()));

        assertThat(outbox.claim(event.id(), now(), now().minusSeconds(30))).isTrue();
        assertThat(outbox.claim(event.id(), now(), now().minusSeconds(30))).isFalse();

        // Lease expired: the claim used for findPending/claim is now older than the cutoff.
        assertThat(outbox.claim(event.id(), now().plusSeconds(5), now().plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("a published event is marked once and disappears from the pending list")
    void markPublishedIsIdempotent() {
        OutboxEvent event = outbox.append(pending(now()));
        outbox.claim(event.id(), now(), now().minusSeconds(30));

        assertThat(outbox.markPublished(event.id(), now())).isTrue();
        assertThat(outbox.markPublished(event.id(), now())).isFalse();
        assertThat(outbox.findPending(10, now().plusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("deletePublishedBefore removes only published rows")
    void deletePublishedBeforeKeepsPendingEvents() {
        OutboxEvent published = outbox.append(pending(now()));
        outbox.claim(published.id(), now(), now().minusSeconds(30));
        outbox.markPublished(published.id(), now());
        OutboxEvent pending = outbox.append(pending(now()));

        int deleted = outbox.deletePublishedBefore(now().plusSeconds(1));

        assertThat(deleted).isEqualTo(1);
        assertThat(outbox.findPending(10, now())).extracting(OutboxEvent::id)
                .containsExactly(pending.id());
    }

    /**
     * Postgres stores timestamps with microsecond precision, and java.time works
     * in nanoseconds: truncating here keeps the round-trip comparison exact.
     */
    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * The payload must be valid JSON: the column is JSONB and Postgres rejects
     * anything else on insert.
     */
    private static OutboxEvent pending(LocalDateTime createdAt) {
        return OutboxEvent.pending(OutboxAggregateTypes.MOVEMENT, 42L,
                OutboxEventTypes.MONEY_TRANSFERRED,
                "{\"movementId\":42,\"amountCents\":1000}", createdAt);
    }
}
