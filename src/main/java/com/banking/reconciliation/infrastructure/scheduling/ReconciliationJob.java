package com.banking.reconciliation.infrastructure.scheduling;

import com.banking.reconciliation.application.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly trigger of the reconciliation check. The logic lives in
 * {@link ReconciliationService}; this class only decides when it runs.
 *
 * 03:30: the idempotency purge owns 03:00 and the outbox purge 04:00.
 */
@Component
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final ReconciliationService service;

    public ReconciliationJob(ReconciliationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${reconciliation.cron:0 30 3 * * *}")
    public int reconcile() {
        try {
            return service.reconcileOnce();
        } catch (RuntimeException e) {
            // A failed run must not break the scheduler: the next one is a day
            // away and no data has been touched (the service is transactional).
            log.warn("Reconciliation failed; will retry on next run", e);
            return 0;
        }
    }
}
