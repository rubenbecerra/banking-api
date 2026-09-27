package com.banking.transactions.application.usecase;

import com.banking.transactions.domain.model.Transaction;
import java.math.BigDecimal;

public interface DepositPort {
    OperationResult<Transaction> deposit(String targetIban, BigDecimal amount, String ownerEmail);
    OperationResult<Transaction> adminDeposit(String targetIban, BigDecimal amount);
}