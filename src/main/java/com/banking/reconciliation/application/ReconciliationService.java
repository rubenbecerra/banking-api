package com.banking.reconciliation.application;

import com.banking.reconciliation.domain.model.Discrepancy;
import com.banking.reconciliation.domain.repository.ReconciliationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Compares, for every account, the stored balance with the ledger.
 *
 * It only reports: repairing a balance automatically would erase the evidence
 * of where the money went. The whole run is one transaction so the
 * "detect + upsert + resolve" set is consistent with itself.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final ReconciliationRepository repository;
    private final Clock clock;

    public ReconciliationService(ReconciliationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Runs the check once.
     *
     * @return number of accounts currently discrepant
     */
    @Transactional
    public int reconcileOnce() {
        LocalDateTime now = LocalDateTime.now(clock);

        // Money conservation: with double-entry entries this is arithmetic, so
        // a non-zero value points to a bug in how entries are written.
        BigDecimal net = repository.ledgerNet();
        if (net.signum() != 0) {
            log.warn("Ledger is not balanced: net={}", net);
        }

        List<Discrepancy> discrepancies = repository.findDiscrepancies();
        repository.upsertOpen(discrepancies, now);

        List<Long> stillDiscrepant = discrepancies.stream().map(Discrepancy::accountId).toList();
        int resolved = repository.resolveAllExcept(stillDiscrepant, now);

        if (!discrepancies.isEmpty()) {
            log.warn("Reconciliation found {} account(s) with balance != ledger", discrepancies.size());
            discrepancies.forEach(d -> log.warn("  account {} ({}): balance={}, ledgerNet={}, difference={}",
                    d.accountId(), d.iban(), d.balance(), d.ledgerNet(), d.difference()));
        }
        if (resolved > 0) {
            log.info("Reconciliation resolved {} previous discrepanc(ies)", resolved);
        }
        return discrepancies.size();
    }
}
