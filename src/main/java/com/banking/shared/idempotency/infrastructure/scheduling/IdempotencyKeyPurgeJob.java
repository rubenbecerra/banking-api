package com.banking.shared.idempotency.infrastructure.scheduling;

import com.banking.shared.idempotency.domain.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Deletes idempotency keys past their retention window.
 *
 * The cutoff uses the same Clock that wrote expires_at (the application
 * clock). The previous version compared application-written timestamps
 * against the database clock, which only works while both machines agree.
 *
 * Running it twice is harmless: the second run deletes zero rows.
 */
@Component
public class IdempotencyKeyPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyPurgeJob.class);

    private final IdempotencyKeyRepository repository;
    private final Clock clock;

    public IdempotencyKeyPurgeJob(IdempotencyKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${idempotency.purge.cron:0 0 3 * * *}")
    public int purgeExpired() {
        try {
            int deleted = repository.deleteExpiredBefore(LocalDateTime.now(clock));
            if (deleted > 0) {
                log.info("Purged {} expired idempotency keys", deleted);
            }
            return deleted;
        } catch (RuntimeException e) {
            // A failed purge must not break the scheduler: expired keys just
            // stay one more day and the next run is a day away.
            log.warn("Idempotency purge failed; will retry on next run", e);
            return 0;
        }
    }
}
