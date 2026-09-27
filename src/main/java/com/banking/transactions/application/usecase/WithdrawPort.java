package com.banking.transactions.application.usecase;

import com.banking.transactions.domain.model.Transaction;

import java.math.BigDecimal;

public interface WithdrawPort {
    OperationResult<Transaction> withdraw(String ownerEmail, String sourceIban, BigDecimal amount);
    OperationResult<Transaction> adminWithdraw(String sourceIban, BigDecimal amount);
}