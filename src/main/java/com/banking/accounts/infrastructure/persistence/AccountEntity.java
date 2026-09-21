package com.banking.accounts.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String iban;
    private BigDecimal balance;
    private String owner;

    public AccountEntity() {}

    public AccountEntity(Long id, String iban, BigDecimal balance, String owner) {
        this.id = id;
        this.iban = iban;
        this.balance = balance;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getIban() { return iban; }
    public BigDecimal getBalance() { return balance; }
    public String getOwner() { return owner; }
}