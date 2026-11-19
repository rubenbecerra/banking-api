package com.banking.accounts.application.usecase;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.ledger.domain.model.LedgerEntry;
import com.banking.ledger.domain.model.Movement;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import com.banking.ledger.domain.repository.MovementRepository;
import com.banking.shared.util.IbanGenerator;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Opens an account. An initial balance is money entering the bank from outside,
 * so it is booked as a DEPOSIT movement against EXTERNAL, with its two ledger
 * entries, in the same transaction as the account row.
 *
 * Without those entries the account would have a balance the ledger cannot
 * explain, and the reconciliation job would (correctly) report it.
 *
 * It is deliberately NOT a row in `transactions`: that table is the customer's
 * history of operations, and an opening balance is provisioning, not something
 * the customer did. The ledger keeps the money story; the history keeps the
 * user story.
 */
public class CreateAccountUseCase implements CreateAccountPort {

    private final AccountRepository repo;
    private final MovementRepository movementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionTemplate transactionTemplate;

    public CreateAccountUseCase(AccountRepository repo,
                                MovementRepository movementRepository,
                                LedgerEntryRepository ledgerEntryRepository,
                                TransactionTemplate transactionTemplate) {
        this.repo = repo;
        this.movementRepository = movementRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Account execute(String ownerEmail, BigDecimal balance) {
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        return transactionTemplate.execute(status -> open(ownerEmail, balance));
    }

    private Account open(String ownerEmail, BigDecimal balance) {
        Account account = repo.save(new Account(IbanGenerator.generateSpanishIban(), balance, ownerEmail));

        if (balance.signum() == 0) {
            return account;
        }

        Movement movement = movementRepository.save(Movement.createDeposit(account.getIban(), balance));
        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createDebit(movement.getId(), LedgerEntry.EXTERNAL_ACCOUNT, balance),
                LedgerEntry.createCredit(movement.getId(), account.getIban(), balance)
        ));
        return account;
    }
}
