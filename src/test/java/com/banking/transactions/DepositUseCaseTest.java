package com.banking.transactions;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.ledger.domain.model.EntryDirection;
import com.banking.ledger.domain.model.LedgerEntry;
import com.banking.ledger.domain.model.Movement;
import com.banking.ledger.domain.model.MovementType;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import com.banking.ledger.domain.repository.MovementRepository;
import com.banking.transactions.application.usecase.DepositUseCase;
import com.banking.transactions.domain.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositUseCaseTest {

    private static final String IBAN = "ES1234567890123456789012";
    private static final String OWNER = "owner@gmail.com";

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MovementRepository movementRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks private DepositUseCase useCase;

    @Captor private ArgumentCaptor<Movement> movementCaptor;
    @Captor private ArgumentCaptor<List<LedgerEntry>> entriesCaptor;

    @Test
    @DisplayName("A deposit posts a DEPOSIT movement and two entries against EXTERNAL")
    void depositShouldPostDepositMovementAndBalancedEntries() {
        // given
        Account account = new Account(IBAN, new BigDecimal("100.00"), OWNER);
        when(accountRepository.findByIbanWithLock(IBAN)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any(Movement.class))).thenAnswer(inv -> {
            Movement m = inv.getArgument(0);
            return new Movement(1L, m.getType(), m.getSourceIban(), m.getTargetIban(),
                    m.getAmount(), m.getCreatedAt());
        });

        // when
        useCase.deposit(IBAN, new BigDecimal("40.00"), OWNER);

        // then
        verify(transactionRepository).save(any());
        verify(accountRepository).save(account);

        verify(movementRepository).save(movementCaptor.capture());
        Movement savedMovement = movementCaptor.getValue();

        assertThat(savedMovement.getType()).isEqualTo(MovementType.DEPOSIT);
        assertThat(savedMovement.getSourceIban()).isNull();
        assertThat(savedMovement.getTargetIban()).isEqualTo(IBAN);
        assertThat(savedMovement.getAmount()).isEqualByComparingTo("40.00");

        verify(ledgerEntryRepository).saveAll(entriesCaptor.capture());
        List<LedgerEntry> entries = entriesCaptor.getValue();

        assertThat(entries).hasSize(2);

        LedgerEntry debit = entries.stream()
                .filter(entry -> entry.getDirection() == EntryDirection.DEBIT)
                .findFirst()
                .orElseThrow();
        LedgerEntry credit = entries.stream()
                .filter(entry -> entry.getDirection() == EntryDirection.CREDIT)
                .findFirst()
                .orElseThrow();

        assertThat(debit.getAccountIban()).isEqualTo(LedgerEntry.EXTERNAL_ACCOUNT);
        assertThat(credit.getAccountIban()).isEqualTo(IBAN);
        assertThat(debit.getAmount()).isEqualByComparingTo("40.00");
        assertThat(credit.getAmount()).isEqualByComparingTo("40.00");
        assertThat(debit.getMovementId()).isEqualTo(1L);
        assertThat(credit.getMovementId()).isEqualTo(1L);

        assertThat(account.getBalance()).isEqualByComparingTo("140.00");
    }
}