package com.banking.transactions.application.usecase;

import com.banking.transactions.domain.model.Transaction;

import java.math.BigDecimal;

public interface DepositPort {
    Transaction deposit(String targetIban, BigDecimal amount, String ownerEmail);
    Transaction adminDeposit(String targetIban, BigDecimal amount);
}