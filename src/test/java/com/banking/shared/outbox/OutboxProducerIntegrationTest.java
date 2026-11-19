package com.banking.shared.outbox;

import com.banking.accounts.application.usecase.CreateAccountPort;
import com.banking.accounts.domain.model.Account;
import com.banking.accounts.infrastructure.persistence.SpringDataAccountRepository;
import com.banking.customer.AbstractTestContainers;
import com.banking.ledger.infrastructure.persistence.SpringDataLedgerEntryRepository;
import com.banking.ledger.infrastructure.persistence.SpringDataMovementRepository;
import com.banking.ledger.domain.model.MovementType;
import com.banking.ledger.infrastructure.persistence.MovementEntity;
import com.banking.shared.idempotency.infrastructure.persistence.SpringDataIdempotencyKeyRepository;
import com.banking.shared.outbox.domain.repository.OutboxEventRepository;
import com.banking.shared.outbox.infrastructure.persistence.OutboxEventEntity;
import com.banking.shared.outbox.infrastructure.persistence.SpringDataOutboxEventRepository;
import com.banking.shared.security.IdempotencyContextHolder;
import com.banking.transactions.application.usecase.OperationResult;
import com.banking.transactions.application.usecase.TransferMoneyPort;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.infrastructure.persistence.SpringDataTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Producer side of the outbox: the event is born in the same transaction as
 * the money. Four claims:
 *   1. a committed transfer writes exactly ONE EVENT ROW (not "exactly once"
 *      delivery: the relay delivers at-least-once, dedup is the consumer's job);
 *   2. a rejected transfer writes nothing (no event, no money, no transaction);
 *   3. a retry with the same Idempotency-Key does not write a second event;
 *   4. if the outbox insert fails, the whole transfer rolls back.
 */
class OutboxProducerIntegrationTest extends AbstractTestContainers {

    @Autowired
    private TransferMoneyPort transferMoneyPort;

    @Autowired
    private CreateAccountPort createAccountPort;

    @Autowired
    private SpringDataOutboxEventRepository outboxRows;

    @Autowired
    private SpringDataMovementRepository movementRows;

    @Autowired
    private SpringDataLedgerEntryRepository ledgerEntryRows;

    @Autowired
    private SpringDataTransactionRepository transactionRows;

    @Autowired
    private SpringDataAccountRepository accountRows;

    @Autowired
    private SpringDataIdempotencyKeyRepository idempotencyRows;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Spy on the port, not on the JPA repository: the use case must fail where it
     * really writes the event, and the rest of the transaction must roll back.
     */
    @MockitoSpyBean
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void clean() {
        // ledger_entries references movements: delete the entries first.
        outboxRows.deleteAll();
        ledgerEntryRows.deleteAll();
        movementRows.deleteAll();
        transactionRows.deleteAll();
        idempotencyRows.deleteAll();
        accountRows.deleteAll();
    }

    @AfterEach
    void clearIdempotencyContext() {
        IdempotencyContextHolder.clear();
    }

    @Test
    @DisplayName("a committed transfer writes exactly one event row, bound to its movement")
    void transferWritesExactlyOneEventRow() throws Exception {
        Account source = createAccountPort.execute("sender@gmail.com", new BigDecimal("100"));
        Account target = createAccountPort.execute("receiver@gmail.com", new BigDecimal("50"));

        transferMoneyPort.transferMoney("sender@gmail.com",
                source.getIban(), target.getIban(), new BigDecimal("40"));

        List<OutboxEventEntity> events = outboxRows.findAll();
        assertThat(events).hasSize(1);
        OutboxEventEntity event = events.get(0);

        List<Long> movementIds = movementRows.findAll().stream()
                .filter(m -> m.getType() == MovementType.TRANSFER)
                .map(MovementEntity::getId)
                .toList();
        assertThat(movementIds).hasSize(1);
        long movementId = movementIds.get(0);

        // Literals on purpose: these two values are the contract with consumers.
        // Comparing against OutboxAggregateTypes/OutboxEventTypes would also pass
        // if someone changed the constant, which is exactly what must not happen.
        assertThat(event.getAggregateType()).isEqualTo("MOVEMENT");
        assertThat(event.getAggregateId()).isEqualTo(movementId);
        assertThat(event.getEventType()).isEqualTo("MoneyTransferred");
        assertThat(event.getCreatedAt()).isNotNull();
        // Just born: nobody has claimed it and nobody has published it.
        assertThat(event.getClaimedAt()).isNull();
        assertThat(event.getPublishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("movementId").asLong()).isEqualTo(movementId);
        assertThat(payload.get("fromIban").asText()).isEqualTo(source.getIban());
        assertThat(payload.get("toIban").asText()).isEqualTo(target.getIban());
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("40");
        assertThat(payload.get("occurredAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("a rejected transfer writes nothing: no event, no money")
    void failedTransferLeavesNoEvent() {
        long transactionsBefore = transactionRows.count();
        Account source = createAccountPort.execute("sender@gmail.com", new BigDecimal("100"));
        Account target = createAccountPort.execute("receiver@gmail.com", new BigDecimal("50"));
        // Opening balances are ledger-backed: compare against the setup, not zero.
        long movementsBefore = movementRows.count();
        long entriesBefore = ledgerEntryRows.count();

        assertThatThrownBy(() -> transferMoneyPort.transferMoney("sender@gmail.com",
                source.getIban(), target.getIban(), new BigDecimal("500")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(outboxRows.findAll()).isEmpty();
        assertThat(movementRows.count()).isEqualTo(movementsBefore);
        assertThat(ledgerEntryRows.count()).isEqualTo(entriesBefore);
        assertThat(transactionRows.count()).isEqualTo(transactionsBefore);
        // Re-read from the database: the in-memory Account is not the row the
        // use case updated.
        assertThat(balanceOf(source.getIban())).isEqualByComparingTo("100");
        assertThat(balanceOf(target.getIban())).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("a retry with the same Idempotency-Key writes only one event row")
    void retryWithSameKeyWritesOnlyOneEventRow() {
        Account source = createAccountPort.execute("sender@gmail.com", new BigDecimal("100"));
        Account target = createAccountPort.execute("receiver@gmail.com", new BigDecimal("50"));

        IdempotencyContextHolder.setCurrentIdempotencyKey("test-key-1", "sender@gmail.com");

        OperationResult<Transaction> first = transferMoneyPort.transferMoney("sender@gmail.com",
                source.getIban(), target.getIban(), new BigDecimal("40"));

        OperationResult<Transaction> replay = transferMoneyPort.transferMoney("sender@gmail.com",
                source.getIban(), target.getIban(), new BigDecimal("40"));

        assertThat(first.isCacheHit()).isFalse();
        assertThat(replay.isCacheHit()).isTrue();
        assertThat(replay.data().getId()).isEqualTo(first.data().getId());

        assertThat(outboxRows.findAll()).hasSize(1);
        List<MovementEntity> transfers = movementRows.findAll().stream()
                .filter(m -> m.getType() == MovementType.TRANSFER)
                .toList();
        assertThat(transfers).hasSize(1);
        assertThat(ledgerEntryRows.findAll().stream()
                .filter(e -> e.getMovementId().equals(transfers.getFirst().getId()))
                .toList()).hasSize(2);
        // The only proof that matters: the money moved once.
        assertThat(balanceOf(source.getIban())).isEqualByComparingTo("60");
        assertThat(balanceOf(target.getIban())).isEqualByComparingTo("90");
    }

    @Test
    @DisplayName("if the outbox insert fails, the money does not move")
    void outboxFailureRollsBackTheMoney() {
        Account source = createAccountPort.execute("sender@gmail.com", new BigDecimal("100"));
        Account target = createAccountPort.execute("receiver@gmail.com", new BigDecimal("50"));
        long transactionsBefore = transactionRows.count();
        long movementsBefore = movementRows.count();
        long entriesBefore = ledgerEntryRows.count();

        doThrow(new IllegalStateException("outbox unavailable"))
                .when(outboxEventRepository).append(any());

        // The adapter is a @Repository: Spring translates the failure into a
        // DataAccessException, but the cause is still the outbox one.
        assertThatThrownBy(() -> transferMoneyPort.transferMoney("sender@gmail.com",
                source.getIban(), target.getIban(), new BigDecimal("40")))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("outbox unavailable");

        assertThat(outboxRows.count()).isZero();
        assertThat(movementRows.count()).isEqualTo(movementsBefore);
        assertThat(ledgerEntryRows.count()).isEqualTo(entriesBefore);
        assertThat(transactionRows.count()).isEqualTo(transactionsBefore);
        assertThat(balanceOf(source.getIban())).isEqualByComparingTo("100");
        assertThat(balanceOf(target.getIban())).isEqualByComparingTo("50");
    }

    private BigDecimal balanceOf(String iban) {
        return accountRows.findByIban(iban).orElseThrow().getBalance();
    }
}
