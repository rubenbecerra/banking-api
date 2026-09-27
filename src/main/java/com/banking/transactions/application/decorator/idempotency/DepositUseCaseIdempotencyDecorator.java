package com.banking.transactions.application.decorator.idempotency;

import com.banking.shared.infrastructure.idempotency.IdempotencyTemplate;
import com.banking.transactions.application.usecase.DepositPort;
import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.domain.model.Transaction;


import java.math.BigDecimal;

public class DepositUseCaseIdempotencyDecorator implements DepositPort {

    private final DepositPort decoratedPort;
    private final IdempotencyTemplate idempotencyTemplate;

    public DepositUseCaseIdempotencyDecorator(DepositPort decoratedPort, IdempotencyTemplate idempotencyTemplate) {
        this.decoratedPort = decoratedPort;
        this.idempotencyTemplate = idempotencyTemplate;
    }

    @Override
    public OperationResult<Transaction> deposit(String targetIban, BigDecimal amount, String ownerEmail) {
        return idempotencyTemplate.execute("deposit", Transaction.class,
                () -> decoratedPort.deposit(targetIban, amount, ownerEmail)
        );
    }

    @Override
    public OperationResult<Transaction> adminDeposit(String targetIban, BigDecimal amount) {
        return idempotencyTemplate.execute("admin-deposit", Transaction.class,
                () -> decoratedPort.adminDeposit(targetIban, amount)
        );
    }
}
