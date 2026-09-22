package com.banking.transactions.application.usecase;

import com.banking.transactions.domain.model.Transaction;

import java.math.BigDecimal;

public interface TransferMoneyPort {
    Transaction transferMoney(String ownerEmail, String sourceIban, String targetIban, BigDecimal amount);
}