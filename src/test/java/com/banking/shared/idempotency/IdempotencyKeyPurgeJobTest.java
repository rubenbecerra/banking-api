package com.banking.shared.idempotency;

import com.banking.customer.AbstractTestContainers;
import com.banking.shared.idempotency.infrastructure.scheduling.IdempotencyKeyPurgeJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyKeyPurgeJobTest extends AbstractTestContainers {

    @Autowired
    private IdempotencyKeyPurgeJob job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM idempotency_keys");
    }

    @Test
    @DisplayName("Purges expired keys and keeps fresh ones")
    void purgesOnlyExpiredKeys() {
        // The job compares with the application clock, so the fixture uses the
        // same clock instead of the database one.
        LocalDateTime now = LocalDateTime.now();
        insertKey("expired-key", now.minusHours(1));
        insertKey("fresh-key", now.plusHours(1));

        int deleted = job.purgeExpired();

        assertThat(deleted).isEqualTo(1);
        assertThat(countKeys()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT idempotency_key FROM idempotency_keys", String.class))
                .isEqualTo("fresh-key");
    }

    @Test
    @DisplayName("Running the purge twice is harmless")
    void purgeIsIdempotent() {
        insertKey("expired-key", LocalDateTime.now().minusHours(1));

        job.purgeExpired();
        int second = job.purgeExpired();

        assertThat(second).isZero();
        assertThat(countKeys()).isZero();
    }

    private void insertKey(String key, LocalDateTime expiresAt) {
        // created_at is not compared by the purge; it stays 24 h before the
        // expiry so the row looks like a real one.
        jdbcTemplate.update("""
                INSERT INTO idempotency_keys
                    (principal, scope, idempotency_key, status, request_fingerprint, created_at, expires_at)
                VALUES (?, ?, ?, 'COMPLETED', ?, ?, ?)
                """,
                "purge-test@bank.com", "deposit", key, "fingerprint",
                Timestamp.valueOf(expiresAt.minusHours(24)), Timestamp.valueOf(expiresAt));
    }

    private int countKeys() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys", Integer.class);
        return count == null ? 0 : count;
    }
}
