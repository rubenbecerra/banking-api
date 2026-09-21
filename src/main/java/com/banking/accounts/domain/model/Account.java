package com.banking.accounts.domain.model;

import java.math.BigDecimal;
import java.util.Objects;


public class Account {
    private Long id;
    private String iban;
    private BigDecimal balance;
    private String owner;

    public Account() {}

    public Account(String iban, BigDecimal balance, String owner) {
        this.iban = iban;
        this.balance = balance;
        this.owner = owner;
    }

    public Account(Long id, String iban, BigDecimal balance, String owner) {
        this.id = id;
        this.iban = iban;
        this.balance = balance;
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(iban, account.iban);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(iban);
    }
}
