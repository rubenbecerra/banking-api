package com.banking.transactions.application.usecase;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.exceptions.exception.UnauthorizedActionException;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

public class WithdrawUseCase implements WithdrawPort {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public WithdrawUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Transaction withdraw(String ownerEmail, String sourceIban, BigDecimal amount) {
        Account source = accountRepository.findByIbanWithLock(sourceIban)
                .orElseThrow(() -> new NoSuchElementException("Source account cannot be found"));
        if (!source.getOwner().equals(ownerEmail)) {
            throw new UnauthorizedActionException("You are not the owner of the source account");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the operation");
        }
        source.setBalance(source.getBalance().subtract(amount));

        Transaction transaction = Transaction.createWithdrawal(
                source.getIban(),
                amount
        );
        accountRepository.save(source);

        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction adminWithdraw(String sourceIban, BigDecimal amount) {
        Account source = accountRepository.findByIbanWithLock(sourceIban)
                .orElseThrow(() -> new NoSuchElementException("Source account cannot be found"));

        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the operation");
        }
        source.setBalance(source.getBalance().subtract(amount));

        Transaction transaction = Transaction.createWithdrawal(
                source.getIban(),
                amount
        );
        accountRepository.save(source);

        return transactionRepository.save(transaction);
    }
}