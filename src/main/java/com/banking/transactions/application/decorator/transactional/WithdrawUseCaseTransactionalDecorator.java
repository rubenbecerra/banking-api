package com.banking.transactions.application.decorator.transactional;

import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.application.usecase.WithdrawPort;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

public class WithdrawUseCaseTransactionalDecorator implements WithdrawPort {

    private final WithdrawPort withdrawPort;
    private final TransactionTemplate transactionalTemplate;

    public WithdrawUseCaseTransactionalDecorator(WithdrawPort withdrawPort,
                                                 TransactionTemplate transactionalTemplate) {
        this.withdrawPort = withdrawPort;
        this.transactionalTemplate = transactionalTemplate;
    }

    @Override
    public OperationResult<Transaction> withdraw(String ownerEmail, String sourceIban, BigDecimal amount) {
        return transactionalTemplate.execute(status ->
                withdrawPort.withdraw(ownerEmail,sourceIban,amount));
    }

    @Override
    public OperationResult<Transaction> adminWithdraw(String sourceIban, BigDecimal amount) {
        return transactionalTemplate.execute(status ->
                withdrawPort.adminWithdraw(sourceIban,amount));
    }
}
