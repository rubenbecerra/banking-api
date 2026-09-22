package com.banking.transactions.application.decorator;

import com.banking.transactions.application.usecase.DepositPort;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;


public class DepositUseCaseTransactionalDecorator implements DepositPort {

    private final DepositPort depositPort;

    public DepositUseCaseTransactionalDecorator(DepositPort depositPort) {
        this.depositPort = depositPort;
    }

    @Override
    @Transactional
    public Transaction deposit(String targetIban, BigDecimal amount, String ownerEmail) {
        return depositPort.deposit(targetIban, amount, ownerEmail);
    }

    @Override
    @Transactional
    public Transaction adminDeposit(String targetIban, BigDecimal amount) {
        return depositPort.adminDeposit(targetIban, amount);
    }
}