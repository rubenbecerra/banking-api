package com.banking.reconciliation.domain.model;

import java.math.BigDecimal;

/**
 * One account whose balance and ledger disagree.
 *
 * @param accountId accounts.id
 * @param iban      the account identifier, kept even if the account is deleted
 * @param balance   accounts.balance at detection time
 * @param ledgerNet sum(credits) - sum(debits) at detection time
 */
public record Discrepancy(long accountId, String iban, BigDecimal balance, BigDecimal ledgerNet) {

    /** How much the stored balance is off. Never zero for a real discrepancy. */
    public BigDecimal difference() {
        return balance.subtract(ledgerNet);
    }
}
