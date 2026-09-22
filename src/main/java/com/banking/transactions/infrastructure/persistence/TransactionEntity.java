package com.banking.transactions.infrastructure.persistence;

import com.banking.transactions.domain.model.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_iban")
    private String sourceIban;

    @Column(name = "target_iban")
    private String targetIban;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public TransactionEntity() {}

    public TransactionEntity(String sourceIban, String targetIban, BigDecimal amount, TransactionType type) {
        this.sourceIban = sourceIban;
        this.targetIban = targetIban;
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSourceIban() { return sourceIban; }
    public String getTargetIban() { return targetIban; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setId(Long id) { this.id = id; }
    public void setSourceIban(String sourceIban) { this.sourceIban = sourceIban; }
    public void setTargetIban(String targetIban) { this.targetIban = targetIban; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setType(TransactionType type) { this.type = type; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}