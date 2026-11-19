package com.banking.shared.outbox.infrastructure.scheduling;

import com.banking.shared.outbox.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Deletes published outbox events past their retention window.
 *
 * Only published rows are ever deleted: a pending event is undelivered work
 * and survives no matter how old it is. Published rows serve auditing and
 * debugging only, hence a short retention (7 days by default).
 *
 * The cutoff uses the same Clock that writes published_at (the application
 * clock), never the database clock: purge, relay and rows then agree on what
 * "now" means.
 *
 * Known limitation, fine for this volume: the delete is a single statement.
 * With millions of published rows it should run in batches to keep each
 * transaction short.
 */
@Component
public class OutboxPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPurgeJob.class);

    private final OutboxEventRepository repository;
    private final Clock clock;
    private final int retentionDays;

    public OutboxPurgeJob(OutboxEventRepository repository,
                          Clock clock,
                          @Value("${outbox.purge.retention-days:7}") int retentionDays) {
        if (retentionDays <= 0) {
            // A zero or negative window would delete the whole audit history
            // on the first run. Fail at startup instead.
            throw new IllegalArgumentException(
                    "outbox.purge.retention-days must be positive, got: " + retentionDays);
        }
        this.repository = repository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${outbox.purge.cron:0 0 4 * * *}")
    public int purgePublished() {
        try {
            LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
            int deleted = repository.deletePublishedBefore(cutoff);
            if (deleted > 0) {
                log.info("Purged {} published outbox events older than {} days",
                        deleted, retentionDays);
            }
            return deleted;
        } catch (RuntimeException e) {
            // A failed purge must not break the scheduler: pending events are
            // never touched and the next run is a day away.
            log.warn("Outbox purge failed; will retry on next run", e);
            return 0;
        }
    }
}
