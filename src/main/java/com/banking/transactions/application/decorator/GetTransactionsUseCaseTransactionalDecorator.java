package com.banking.transactions.application.decorator;

import com.banking.transactions.application.usecase.GetTransactionsPort;
import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.transaction.annotation.Transactional;

public class GetTransactionsUseCaseTransactionalDecorator implements GetTransactionsPort {

    private final GetTransactionsPort getTransactionsPort;

    public GetTransactionsUseCaseTransactionalDecorator(GetTransactionsPort getTransactionsPort) {
        this.getTransactionsPort = getTransactionsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Transaction> getTransactionsForAccount(String iban, String authenticatedEmail, int page, int size, String sortBy) {
        return getTransactionsPort.getTransactionsForAccount(iban, authenticatedEmail, page, size, sortBy);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Transaction> adminGetTransactionsByIban(String iban, int page, int size, String sortBy) {
        return getTransactionsPort.adminGetTransactionsByIban(iban, page, size, sortBy);
    }
}
