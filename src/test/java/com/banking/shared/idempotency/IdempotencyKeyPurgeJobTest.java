package com.banking.shared.idempotency;

import com.banking.customer.AbstractTestContainers;
import com.banking.shared.idempotency.application.IdempotencyKeyPurgeJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

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
        insertKey("expired-key", "CURRENT_TIMESTAMP - INTERVAL '1 hour'");
        insertKey("fresh-key", "CURRENT_TIMESTAMP + INTERVAL '1 hour'");

        job.purgeExpired();

        assertThat(countKeys()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT idempotency_key FROM idempotency_keys", String.class))
                .isEqualTo("fresh-key");
    }

    @Test
    @DisplayName("Running the purge twice is harmless")
    void purgeIsIdempotent() {
        insertKey("expired-key", "CURRENT_TIMESTAMP - INTERVAL '1 hour'");

        job.purgeExpired();
        job.purgeExpired();

        assertThat(countKeys()).isZero();
    }

    private void insertKey(String key, String expiresAtExpression) {
        jdbcTemplate.update("""
                INSERT INTO idempotency_keys
                    (principal, scope, idempotency_key, status, request_fingerprint, created_at, expires_at)
                VALUES (?, ?, ?, 'COMPLETED', ?, CURRENT_TIMESTAMP, %s)
                """.formatted(expiresAtExpression),
                "purge-test@bank.com", "deposit", key, "fingerprint");
    }

    private int countKeys() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys", Integer.class);
        return count == null ? 0 : count;
    }
}
