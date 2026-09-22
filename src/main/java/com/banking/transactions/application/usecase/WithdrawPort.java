package com.banking.transactions.application.usecase;

import com.banking.transactions.domain.model.Transaction;

import java.math.BigDecimal;

public interface WithdrawPort {
    Transaction withdraw(String ownerEmail, String sourceIban, BigDecimal amount);
    Transaction adminWithdraw(String sourceIban, BigDecimal amount);
}