package com.banking.transactions.application.decorator.transactional;

import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.application.usecase.TransferMoneyPort;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;


public class TransferMoneyUseCaseTransactionalDecorator implements TransferMoneyPort {

    private final TransferMoneyPort transferMoneyPort;
    private final TransactionTemplate transactionTemplate;

    public TransferMoneyUseCaseTransactionalDecorator(TransferMoneyPort transferMoneyPort,
                                                      TransactionTemplate transactionTemplate) {
        this.transferMoneyPort = transferMoneyPort;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public OperationResult<Transaction> transferMoney(String ownerEmail,
                                                      String sourceIban,
                                                      String targetIban,
                                                      BigDecimal amount) {
        return transactionTemplate.execute(status ->
                transferMoneyPort.transferMoney(ownerEmail,sourceIban,targetIban,amount));
    }
}