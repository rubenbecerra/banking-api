package com.banking.transactions.application.decorator.idempotency;

import com.banking.shared.infrastructure.idempotency.IdempotencyTemplate;
import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.application.usecase.TransferMoneyPort;
import com.banking.transactions.domain.model.Transaction;

import java.math.BigDecimal;

public class TransferUseCaseIdempotencyDecorator implements TransferMoneyPort {

    private final TransferMoneyPort decoratedPort;
    private final IdempotencyTemplate idempotencyTemplate;

    public TransferUseCaseIdempotencyDecorator(TransferMoneyPort decoratedPort,
                                               IdempotencyTemplate idempotencyTemplate) {
        this.decoratedPort = decoratedPort;
        this.idempotencyTemplate = idempotencyTemplate;
    }

    @Override
    public OperationResult<Transaction> transferMoney(String ownerEmail,
                                                      String sourceIban,
                                                      String targetIban,
                                                      BigDecimal amount) {
        return idempotencyTemplate.execute(
                "transfer",
                fingerprint(sourceIban, targetIban, amount),
                Transaction.class,
                () -> decoratedPort.transferMoney(ownerEmail, sourceIban, targetIban, amount)
        );
    }

    private String fingerprint(String sourceIban, String targetIban, BigDecimal amount) {
        return sourceIban + "|" + targetIban + "|" + amount.stripTrailingZeros().toPlainString();
    }
}
