package com.banking.transactions.application.decorator.idempotency;

import com.banking.shared.infrastructure.idempotency.IdempotencyTemplate;
import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.application.usecase.WithdrawPort;
import com.banking.transactions.domain.model.Transaction;

import java.math.BigDecimal;

public class WithdrawUseCaseIdempotencyDecorator implements WithdrawPort {

    private final WithdrawPort decoratedPort;
    private final IdempotencyTemplate idempotencyTemplate;

    public WithdrawUseCaseIdempotencyDecorator(WithdrawPort decoratedPort,
                                               IdempotencyTemplate idempotencyTemplate) {
        this.decoratedPort = decoratedPort;
        this.idempotencyTemplate = idempotencyTemplate;
    }

    @Override
    public OperationResult<Transaction> withdraw(String ownerEmail, String sourceIban, BigDecimal amount) {
        return idempotencyTemplate.execute(
                "withdraw",
                fingerprint(sourceIban, amount),
                Transaction.class,
                () -> decoratedPort.withdraw(ownerEmail, sourceIban, amount)
        );
    }

    @Override
    public OperationResult<Transaction> adminWithdraw(String sourceIban, BigDecimal amount) {
        return idempotencyTemplate.execute(
                "admin-withdraw",
                fingerprint(sourceIban, amount),
                Transaction.class,
                () -> decoratedPort.adminWithdraw(sourceIban, amount)
        );
    }

    private String fingerprint(String sourceIban, BigDecimal amount) {
        return sourceIban + "|" + amount.stripTrailingZeros().toPlainString();
    }
}
