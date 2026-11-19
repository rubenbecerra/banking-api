package com.banking.reconciliation.domain.repository;

import com.banking.reconciliation.domain.model.Discrepancy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Reconciliation persistence.
 *
 * The cutoff times come from the caller (application clock), so the adapter can
 * be tested with fixed instants and never decides what "now" means.
 */
public interface ReconciliationRepository {

    /**
     * Accounts whose balance differs from the ledger total, oldest id first.
     * One single query: loading every account into memory to sum its entries is
     * the N+1 version of this check.
     */
    List<Discrepancy> findDiscrepancies();

    /**
     * Inserts a discrepancy or refreshes the open one for the same account
     * (difference and last_seen_at). Never touches resolved rows.
     */
    void upsertOpen(List<Discrepancy> discrepancies, LocalDateTime seenAt);

    /**
     * Resolves the open discrepancies whose account is not in
     * {@code stillDiscrepantAccountIds}: fixed, or the account no longer exists.
     * An empty collection resolves every open row.
     *
     * @return number of rows resolved
     */
    int resolveAllExcept(Collection<Long> stillDiscrepantAccountIds, LocalDateTime resolvedAt);

    /**
     * Net of the whole ledger: sum(credits) - sum(debits). Must be exactly zero;
     * anything else means money was created or destroyed between accounts.
     */
    BigDecimal ledgerNet();
}
