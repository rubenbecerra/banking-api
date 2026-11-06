package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.domain.model.EntryDirection;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movement_id", nullable = false)
    private Long movementId;

    @Column(name = "account_iban", nullable = false, length = 24)
    private String accountIban;

    @Convert(converter = EntryDirectionConverter.class)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "direction", nullable = false, length = 1)
    private EntryDirection direction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LedgerEntryEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMovementId() {
        return movementId;
    }

    public void setMovementId(Long movementId) {
        this.movementId = movementId;
    }

    public String getAccountIban() {
        return accountIban;
    }

    public void setAccountIban(String accountIban) {
        this.accountIban = accountIban;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public void setDirection(EntryDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
