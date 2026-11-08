package com.banking.transactions.application.usecase;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.ledger.domain.model.LedgerEntry;
import com.banking.ledger.domain.model.Movement;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import com.banking.ledger.domain.repository.MovementRepository;
import com.banking.shared.exceptions.exception.UnauthorizedActionException;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

public class WithdrawUseCase implements WithdrawPort {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WithdrawUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository,
                           MovementRepository movementRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public OperationResult<Transaction> withdraw(String ownerEmail, String sourceIban, BigDecimal amount) {
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

        Movement movement = movementRepository.save(Movement.createWithdrawal(source.getIban(), amount));

        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createCredit(movement.getId(), LedgerEntry.EXTERNAL_ACCOUNT, amount),
                LedgerEntry.createDebit(movement.getId(), source.getIban(), amount)
        ));

        Transaction savedTransaction = transactionRepository.save(transaction);
        return OperationResult.fresh(savedTransaction);
    }

    @Override
    public OperationResult<Transaction> adminWithdraw(String sourceIban, BigDecimal amount) {
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

        Movement movement = movementRepository.save(Movement.createWithdrawal(source.getIban(), amount));
        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createCredit(movement.getId(), LedgerEntry.EXTERNAL_ACCOUNT, amount),
                LedgerEntry.createDebit(movement.getId(), source.getIban(), amount)
        ));

        Transaction savedTransaction = transactionRepository.save(transaction);
        return OperationResult.fresh(savedTransaction);
    }
}