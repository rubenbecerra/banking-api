package com.banking.transactions.application.decorator;

import com.banking.transactions.application.usecase.TransferMoneyPort;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;


public class TransferMoneyUseCaseTransactionalDecorator implements TransferMoneyPort {

    private final TransferMoneyPort transferMoneyPort;

    public TransferMoneyUseCaseTransactionalDecorator(TransferMoneyPort transferMoneyPort) {
        this.transferMoneyPort = transferMoneyPort;
    }

    @Override
    @Transactional
    public Transaction transferMoney(String ownerEmail, String sourceIban, String targetIban, BigDecimal amount) {
        return transferMoneyPort.transferMoney(ownerEmail, sourceIban, targetIban, amount);
    }
}