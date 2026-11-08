package com.banking.transactions.application.usecase;

import com.banking.ledger.domain.model.LedgerEntry;
import com.banking.ledger.domain.model.Movement;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import com.banking.ledger.domain.repository.MovementRepository;
import com.banking.shared.exceptions.exception.AccountNotFoundException;
import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.exceptions.exception.UnauthorizedActionException;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;

public class DepositUseCase implements DepositPort {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public DepositUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository,
                          MovementRepository movementRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public OperationResult<Transaction> deposit(String targetIban, BigDecimal amount, String ownerEmail) {
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
        Transaction savedTransaction = transactionRepository.save(transaction);

        Movement movement = movementRepository.save(Movement.createDeposit(target.getIban(), amount));
        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createDebit(movement.getId(), LedgerEntry.EXTERNAL_ACCOUNT, amount),
                LedgerEntry.createCredit(movement.getId(), target.getIban(), amount)
        ));

        return OperationResult.fresh(savedTransaction);
    }

    @Override
    public OperationResult<Transaction> adminDeposit(String targetIban, BigDecimal amount) {
        Account target = accountRepository.findByIbanWithLock(targetIban)
                .orElseThrow(() -> new AccountNotFoundException("Target account cannot be found"));
        target.setBalance(target.getBalance().add(amount));
        Transaction transaction = Transaction.createDeposit(
                target.getIban(),
                amount
        );
        accountRepository.save(target);
        Transaction savedTransaction = transactionRepository.save(transaction);

        Movement movement = movementRepository.save(Movement.createDeposit(target.getIban(), amount));
        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createDebit(movement.getId(), LedgerEntry.EXTERNAL_ACCOUNT, amount),
                LedgerEntry.createCredit(movement.getId(), target.getIban(), amount)
        ));

        return OperationResult.fresh(savedTransaction);
    }
}