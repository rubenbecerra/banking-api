package com.banking.transactions.application.decorator.transactional;

import com.banking.transactions.application.usecase.DepositPort;
import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;

public class DepositUseCaseTransactionalDecorator implements DepositPort {

    private final DepositPort depositPort;
    private final TransactionTemplate transactionTemplate;

    public DepositUseCaseTransactionalDecorator(DepositPort depositPort, TransactionTemplate transactionTemplate) {
        this.depositPort = depositPort;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public OperationResult<Transaction> deposit(String targetIban, BigDecimal amount, String ownerEmail) {
        return transactionTemplate.execute(status -> depositPort.deposit(targetIban, amount, ownerEmail));
    }

    @Override
    public OperationResult<Transaction> adminDeposit(String targetIban, BigDecimal amount) {
        return transactionTemplate.execute(status -> depositPort.adminDeposit(targetIban, amount));
    }
}