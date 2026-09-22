package com.banking.transactions.application.usecase;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.exceptions.exception.UnauthorizedActionException;
import com.banking.transactions.domain.model.PageQuery;
import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.repository.TransactionRepository;

import java.util.NoSuchElementException;

public class GetTransactionsUseCase implements GetTransactionsPort {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public GetTransactionsUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public PageResult<Transaction> getTransactionsForAccount(String iban, String authenticatedEmail,
                                                             int page, int size, String sortBy) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementException("Account cannot be found"));
        if (!account.getOwner().equals(authenticatedEmail)) {
            throw new UnauthorizedActionException("You are not authorized to view transactions for this account");
        }
        PageQuery pageQuery = new PageQuery(page,size,sortBy);
        return transactionRepository.findBySourceIbanOrTargetIban(iban, iban, pageQuery);
    }

    @Override
    public PageResult<Transaction> adminGetTransactionsByIban(String iban,
                                                              int page, int size, String sortBy) {
        if (!accountRepository.existsByIban(iban)) {
            throw new NoSuchElementException("Account cannot be found with IBAN: " + iban);
        }
        PageQuery pageQuery = new PageQuery(page,size,sortBy);
        return transactionRepository.findBySourceIbanOrTargetIban(iban, iban, pageQuery);
    }
}