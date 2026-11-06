package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.domain.model.MovementType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movements")
public class MovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 25)
    private MovementType type;

    @Column(name = "source_iban", length = 24)
    private String sourceIban;

    @Column(name = "target_iban", length = 24)
    private String targetIban;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false,updatable = false)
    private LocalDateTime createdAt;

    public MovementEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }

    public String getSourceIban() {
        return sourceIban;
    }

    public void setSourceIban(String sourceIban) {
        this.sourceIban = sourceIban;
    }

    public String getTargetIban() {
        return targetIban;
    }

    public void setTargetIban(String targetIban) {
        this.targetIban = targetIban;
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
