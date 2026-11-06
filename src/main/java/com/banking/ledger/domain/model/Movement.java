package com.banking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movement {
    private Long id;
    private MovementType type;
    private String sourceIban;
    private String targetIban;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public Movement(Long id, MovementType type, String sourceIban, String targetIban, BigDecimal amount,
                     LocalDateTime date) {
        this.id = id;
        this.type = type;
        this.sourceIban = sourceIban;
        this.targetIban = targetIban;
        this.amount = amount;
        this.createdAt = date;
    }

    public static Movement createTransfer(String sourceIban, String targetIban, BigDecimal amount) {
        requirePositiveAmount(amount);

        return new Movement(null,
                MovementType.TRANSFER,
                sourceIban,
                targetIban,
                amount,
                LocalDateTime.now());
    }

    public static Movement createDeposit(String targetIban, BigDecimal amount) {
        requirePositiveAmount(amount);

        return new Movement(null,
                MovementType.DEPOSIT,
                null,
                targetIban,
                amount,
                LocalDateTime.now());
    }

    public static Movement createWithdrawal(String sourceIban, BigDecimal amount) {
        requirePositiveAmount(amount);

        return new Movement(null,
                MovementType.WITHDRAWAL,
                sourceIban,
                null,
                amount,
                LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public MovementType getType() {
        return type;
    }

    public String getTargetIban() {
        return targetIban;
    }

    public String getSourceIban() {
        return sourceIban;
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