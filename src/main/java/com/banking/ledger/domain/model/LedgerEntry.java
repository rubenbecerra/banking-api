package com.banking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerEntry {
    public static final String EXTERNAL_ACCOUNT = "EXTERNAL";

    private Long id;
    private Long movementId;
    private String accountIban;
    private EntryDirection direction;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public LedgerEntry(Long id, Long movementId, String accountIban, EntryDirection direction,
                       BigDecimal amount, LocalDateTime createdAt) {
        this.id = id;
        this.movementId = movementId;
        this.accountIban = accountIban;
        this.direction = direction;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static LedgerEntry createDebit(Long movementId, String accountIban, BigDecimal amount) {
        requirePositiveAmount(amount);
        return new LedgerEntry(
                null,
                movementId,
                accountIban,
                EntryDirection.DEBIT,
                amount,
                LocalDateTime.now()
        );
    }
    public static LedgerEntry createCredit(Long movementId, String accountIban, BigDecimal amount) {
        requirePositiveAmount(amount);
        return new LedgerEntry(
                null,
                movementId,
                accountIban,
                EntryDirection.CREDIT,
                amount,
                LocalDateTime.now()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMovementId() {
        return movementId;
    }

    public String getAccountIban() {
        return accountIban;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
