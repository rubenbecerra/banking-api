package com.banking.ledger;

import com.banking.customer.AbstractTestContainers;
import com.banking.accounts.application.usecase.CreateAccountPort;
import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.ledger.domain.model.EntryDirection;
import com.banking.ledger.domain.model.LedgerEntry;
import com.banking.ledger.domain.model.MovementType;
import com.banking.ledger.infrastructure.persistence.LedgerEntryEntity;
import com.banking.ledger.infrastructure.persistence.MovementEntity;
import com.banking.ledger.infrastructure.persistence.SpringDataLedgerEntryRepository;
import com.banking.ledger.infrastructure.persistence.SpringDataMovementRepository;
import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.application.usecase.TransferMoneyPort;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.model.TransactionType;
import com.banking.transactions.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LedgerPostingIntegrationTest extends AbstractTestContainers {

    @Autowired private TransferMoneyPort transferMoneyPort;
    @Autowired private CreateAccountPort createAccountPort;
    @Autowired private SpringDataMovementRepository movementRepository;
    @Autowired private SpringDataLedgerEntryRepository ledgerEntryRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;

    @BeforeEach
    void clean() {
        ledgerEntryRepository.deleteAll();
        movementRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("A transfer should produce 1 transaction, 1 movement and 2 balanced entries")
    void transferMoneyShouldProduceMovementAndEntries() {
        // Given
        Account source = createAccountPort.execute("sender@gmail.com", new BigDecimal("100"));
        Account target = createAccountPort.execute("receiver@gmail.com", new BigDecimal("50"));

        // When
        OperationResult<Transaction> result = transferMoneyPort.transferMoney(
                "sender@gmail.com",
                source.getIban(),
                target.getIban(),
                new BigDecimal("40"));

        // Then
        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.data().getId()).isNotNull();


        assertThat(transactionRepository.count()).isEqualTo(1);
        Transaction transaction = transactionRepository.findAll().getFirst();
        assertThat(transaction.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(transaction.getSourceIban()).isEqualTo(source.getIban());
        assertThat(transaction.getTargetIban()).isEqualTo(target.getIban());
        assertThat(transaction.getAmount()).isEqualByComparingTo("40");


        assertThat(movementRepository.count()).isEqualTo(1);
        MovementEntity movement = movementRepository.findAll().getFirst();
        assertThat(movement.getId()).isNotNull();
        assertThat(movement.getType()).isEqualTo(MovementType.TRANSFER);
        assertThat(movement.getSourceIban()).isEqualTo(source.getIban());
        assertThat(movement.getTargetIban()).isEqualTo(target.getIban());
        assertThat(movement.getAmount()).isEqualByComparingTo("40");
        assertThat(movement.getCreatedAt()).isNotNull();


        List<LedgerEntryEntity> entries = ledgerEntryRepository.findAll();
        assertThat(entries).hasSize(2);
        assertThat(entries).allSatisfy(
                entry -> assertThat(entry.getMovementId()).isEqualTo(movement.getId()));

        LedgerEntryEntity debit = entries.stream()
                .filter(entry -> entry.getDirection() == EntryDirection.DEBIT)
                .findFirst()
                .orElseThrow();
        LedgerEntryEntity credit = entries.stream()
                .filter(entry -> entry.getDirection() == EntryDirection.CREDIT)
                .findFirst()
                .orElseThrow();

        assertThat(debit.getAccountIban()).isEqualTo(source.getIban());
        assertThat(credit.getAccountIban()).isEqualTo(target.getIban());
        assertThat(debit.getAccountIban()).isNotEqualTo(LedgerEntry.EXTERNAL_ACCOUNT);
        assertThat(debit.getAmount()).isEqualByComparingTo("40");
        assertThat(credit.getAmount()).isEqualByComparingTo("40");
        assertThat(debit.getCreatedAt()).isNotNull();
        assertThat(credit.getCreatedAt()).isNotNull();

        BigDecimal total = entries.stream()
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("80");

        assertThat(accountRepository.findByIban(source.getIban()).orElseThrow().getBalance())
                .isEqualByComparingTo("60");
        assertThat(accountRepository.findByIban(target.getIban()).orElseThrow().getBalance())
                .isEqualByComparingTo("90");
    }

    @Test
    @DisplayName("A failure after writing should roll back every table")
    void givenInvalidAmount_whenTransferFails_thenRollbackEverything() {
        // Given
        Account source = createAccountPort.execute("sender@gmail.com", new BigDecimal("100"));
        Account target = createAccountPort.execute("receiver@gmail.com", new BigDecimal("50"));

        // When
        assertThatThrownBy(() ->
                transferMoneyPort.transferMoney(
                        "sender@gmail.com",
                        source.getIban(),
                        target.getIban(),
                        new BigDecimal("-10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");

        // Then
        assertThat(transactionRepository.count()).isZero();
        assertThat(movementRepository.count()).isZero();
        assertThat(ledgerEntryRepository.count()).isZero();

        assertThat(accountRepository.findByIban(source.getIban()).orElseThrow().getBalance())
                .isEqualByComparingTo("100");
        assertThat(accountRepository.findByIban(target.getIban()).orElseThrow().getBalance())
                .isEqualByComparingTo("50");
    }
}
