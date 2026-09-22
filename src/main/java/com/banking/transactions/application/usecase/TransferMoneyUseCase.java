package com.banking.transactions.application.usecase;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.exceptions.exception.UnauthorizedActionException;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.model.TransactionType;
import com.banking.transactions.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

public class TransferMoneyUseCase implements TransferMoneyPort {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransferMoneyUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Transaction transferMoney(String ownerEmail, String sourceIban, String targetIban, BigDecimal amount) {
        if (sourceIban.equals(targetIban)) {
            throw new IllegalArgumentException("Source and target IBAN cannot be the same");
        }

        Account source;
        Account target;

        if (sourceIban.compareTo(targetIban) < 0) {
            source = accountRepository.findByIbanWithLock(sourceIban)
                    .orElseThrow(() -> new NoSuchElementException("Source account not found"));
            target = accountRepository.findByIbanWithLock(targetIban)
                    .orElseThrow(() -> new NoSuchElementException("Target account not found"));
        } else {
            target = accountRepository.findByIbanWithLock(targetIban)
                    .orElseThrow(() -> new NoSuchElementException("Target account not found"));
            source = accountRepository.findByIbanWithLock(sourceIban)
                    .orElseThrow(() -> new NoSuchElementException("Source account not found"));
        }

        if (!source.getOwner().equals(ownerEmail)) {
            throw new UnauthorizedActionException("You are not the owner of the source account");
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the transfer");
        }

        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(amount));

        Transaction transaction = new Transaction(
                source.getIban(),
                target.getIban(),
                amount,
                TransactionType.TRANSFER
        );

        accountRepository.save(target);
        accountRepository.save(source);

        return transactionRepository.save(transaction);
    }
}