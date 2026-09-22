package com.banking.transactions.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

    private Long id;
    private String sourceIban;
    private String targetIban;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime timestamp;

    public Transaction() {}

    public Transaction(String sourceIban, String targetIban, BigDecimal amount, TransactionType type) {
        this.sourceIban = sourceIban;
        this.targetIban = targetIban;
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(Long id, String sourceIban, String targetIban, BigDecimal amount, TransactionType type, LocalDateTime timestamp) {
        this.id = id;
        this.sourceIban = sourceIban;
        this.targetIban = targetIban;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    public static Transaction createTransfer(String sourceIban, String targetIban, BigDecimal amount) {
        return new Transaction(sourceIban, targetIban, amount, TransactionType.TRANSFER);
    }

    public static Transaction createDeposit(String targetIban, BigDecimal amount) {
        return new Transaction(null, targetIban, amount, TransactionType.DEPOSIT);
    }

    public static Transaction createWithdrawal(String sourceIban, BigDecimal amount) {
        return new Transaction(sourceIban, null, amount, TransactionType.WITHDRAWAL);
    }

    public Long getId() {
        return id;
    }

    public String getSourceIban() {
        return sourceIban;
    }

    public String getTargetIban() {
        return targetIban;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}