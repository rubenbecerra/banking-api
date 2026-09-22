package com.banking.transactions.application.decorator;

import com.banking.transactions.application.usecase.WithdrawPort;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

public class WithdrawUseCaseTransactionalDecorator implements WithdrawPort {

    private final WithdrawPort withdrawPort;

    public WithdrawUseCaseTransactionalDecorator(WithdrawPort withdrawPort) {
        this.withdrawPort = withdrawPort;
    }

    @Override
    @Transactional
    public Transaction withdraw(String ownerEmail, String sourceIban, BigDecimal amount) {
        return withdrawPort.withdraw(ownerEmail, sourceIban, amount);
    }

    @Override
    @Transactional
    public Transaction adminWithdraw(String sourceIban, BigDecimal amount) {
        return withdrawPort.adminWithdraw(sourceIban, amount);
    }
}
