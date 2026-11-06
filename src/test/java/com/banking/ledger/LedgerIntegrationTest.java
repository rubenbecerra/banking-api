package com.banking.ledger;

import com.banking.AbstractTestContainers;
import com.banking.ledger.domain.model.EntryDirection;
import com.banking.ledger.domain.model.LedgerEntry;
import com.banking.ledger.domain.model.Movement;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import com.banking.ledger.domain.repository.MovementRepository;
import com.banking.ledger.infrastructure.persistence.SpringDataLedgerEntryRepository;
import com.banking.ledger.infrastructure.persistence.SpringDataMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerIntegrationTest extends AbstractTestContainers {

    private static final String SOURCE = "ES9121000418450200051332";
    private static final String TARGET = "ES7921000813610123456789";

    @Autowired private MovementRepository movementRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;
    @Autowired private SpringDataLedgerEntryRepository springDataEntryRepo;
    @Autowired private SpringDataMovementRepository springDataMovementRepo;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        springDataEntryRepo.deleteAll();
        springDataMovementRepo.deleteAll();
    }

    @Test
    @DisplayName("Persists one movement and its two ledger entries")
    void persistsMovementAndTwoEntries() {
        Movement movement = movementRepository.save(
                Movement.createTransfer(SOURCE, TARGET, new BigDecimal("100.0000")));

        assertThat(movement.getId()).isNotNull();

        List<LedgerEntry> saved = ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createDebit(movement.getId(), SOURCE, new BigDecimal("100.0000")),
                LedgerEntry.createCredit(movement.getId(), TARGET, new BigDecimal("100.0000"))));

        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(e -> assertThat(e.getId()).isNotNull());

        List<LedgerEntry> found = ledgerEntryRepository.findByMovementId(movement.getId());
        assertThat(found).hasSize(2);
        assertThat(found)
                .extracting(LedgerEntry::getDirection)
                .containsExactlyInAnyOrder(EntryDirection.DEBIT, EntryDirection.CREDIT);

        Long debits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE movement_id = ? AND direction = 'D'",
                Long.class, movement.getId());
        assertThat(debits).isEqualTo(1L);

        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT SUM(amount) FROM ledger_entries WHERE movement_id = ?",
                BigDecimal.class, movement.getId());
        assertThat(total).isEqualByComparingTo("200.0000");
    }

    @Test
    @DisplayName("Rejects a second entry with the same direction for the same movement")
    void rejectsSecondEntryWithSameDirection() {
        Movement movement = movementRepository.save(
                Movement.createTransfer(SOURCE, TARGET, new BigDecimal("50.0000")));

        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createDebit(movement.getId(), SOURCE, new BigDecimal("50.0000")),
                LedgerEntry.createCredit(movement.getId(), TARGET, new BigDecimal("50.0000"))));

        assertThatThrownBy(() -> ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.createDebit(movement.getId(), SOURCE, new BigDecimal("10.0000")))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
