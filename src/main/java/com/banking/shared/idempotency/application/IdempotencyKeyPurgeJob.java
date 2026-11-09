package com.banking.shared.idempotency.application;

import com.banking.shared.idempotency.domain.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes idempotency keys past their retention window.
 *
 * The DELETE uses the database clock (CURRENT_TIMESTAMP), not the JVM clock.
 * Running it twice is harmless: the second run deletes zero rows.
 */
@Component
public class IdempotencyKeyPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyPurgeJob.class);

    private final IdempotencyKeyRepository repository;

    public IdempotencyKeyPurgeJob(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "${idempotency.purge.cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpired() {
        int deleted = repository.deleteExpired();
        log.info("Purged {} expired idempotency keys", deleted);
    }
}
