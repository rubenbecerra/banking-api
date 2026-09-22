package com.banking.transactions.application.usecase;

import com.banking.shared.exceptions.exception.AccountNotFoundException;
import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.exceptions.exception.UnauthorizedActionException;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.repository.TransactionRepository;

import java.math.BigDecimal;

public class DepositUseCase implements DepositPort {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public DepositUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Transaction deposit(String targetIban, BigDecimal amount, String ownerEmail) {
        Account target = accountRepository.findByIbanWithLock(targetIban)
                .orElseThrow(() -> new AccountNotFoundException("Target account cannot be found"));
        if (!target.getOwner().equals(ownerEmail)) {
            throw new UnauthorizedActionException("You can only deposit money into your own accounts");
        }

        target.setBalance(target.getBalance().add(amount));
        Transaction transaction = Transaction.createDeposit(
                target.getIban(),
                amount
        );
        accountRepository.save(target);
        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction adminDeposit(String targetIban, BigDecimal amount) {
        Account target = accountRepository.findByIbanWithLock(targetIban)
                .orElseThrow(() -> new AccountNotFoundException("Target account cannot be found"));
        target.setBalance(target.getBalance().add(amount));
        Transaction transaction = Transaction.createDeposit(
                target.getIban(),
                amount
        );
        accountRepository.save(target);
        return transactionRepository.save(transaction);
    }
}