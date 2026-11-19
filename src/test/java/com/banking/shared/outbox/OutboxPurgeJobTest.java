package com.banking.shared.outbox;

import com.banking.customer.AbstractTestContainers;
import com.banking.shared.outbox.infrastructure.scheduling.OutboxPurgeJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPurgeJobTest extends AbstractTestContainers {

    @Autowired
    private OutboxPurgeJob job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM outbox");
    }

    @Test
    @DisplayName("purges published events older than the retention window only")
    void purgesOnlyOldPublishedEvents() {
        LocalDateTime now = LocalDateTime.now();
        // The invariant: a pending event is never deleted, however old it is.
        // If the WHERE ever changes to created_at, this row dies and the test
        // goes red.
        long pendingOld = insert("PENDING_OLD", now.minusDays(30), null, null);
        long publishedOld = insert("PUBLISHED_OLD",
                now.minusDays(30), now.minusDays(30), now.minusDays(30));
        long publishedRecent = insert("PUBLISHED_RECENT",
                now.minusDays(1), now.minusDays(1), now.minusDays(1));

        int deleted = job.purgePublished();

        assertThat(deleted).isEqualTo(1);
        assertThat(ids()).containsExactlyInAnyOrder(pendingOld, publishedRecent);
    }

    @Test
    @DisplayName("running the purge twice is harmless")
    void purgeIsIdempotent() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        insert("PUBLISHED_OLD", thirtyDaysAgo, thirtyDaysAgo, thirtyDaysAgo);

        job.purgePublished();
        int second = job.purgePublished();

        assertThat(second).isZero();
        assertThat(ids()).isEmpty();
    }

    /**
     * claimed_at and published_at are not writable through the JPA entity
     * (insertable = false, updatable = false), so the rows are seeded with
     * plain SQL. The CHECK chk_outbox_published_after_claim forces a claimed_at
     * whenever published_at is set.
     */
    private long insert(String eventType, LocalDateTime createdAt,
                        LocalDateTime claimedAt, LocalDateTime publishedAt) {
        jdbcTemplate.update("""
                INSERT INTO outbox (aggregate_type, aggregate_id, event_type, payload, created_at, claimed_at, published_at)
                VALUES ('MOVEMENT', 42, ?, '{}'::jsonb, ?, ?, ?)
                """,
                eventType,
                Timestamp.valueOf(createdAt),
                claimedAt == null ? null : Timestamp.valueOf(claimedAt),
                publishedAt == null ? null : Timestamp.valueOf(publishedAt));
        // GENERATED ALWAYS AS IDENTITY: no id comes back from the insert, but
        // event_type is unique per row here, so look the id up by it.
        return jdbcTemplate.queryForObject(
                "SELECT id FROM outbox WHERE event_type = ?", Long.class, eventType);
    }

    private List<Long> ids() {
        return jdbcTemplate.queryForList("SELECT id FROM outbox", Long.class);
    }
}
