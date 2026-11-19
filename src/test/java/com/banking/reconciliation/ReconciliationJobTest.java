package com.banking.reconciliation;

import com.banking.accounts.application.usecase.CreateAccountPort;
import com.banking.accounts.domain.model.Account;
import com.banking.customer.AbstractTestContainers;
import com.banking.reconciliation.infrastructure.scheduling.ReconciliationJob;
import com.banking.transactions.application.usecase.DepositPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationJobTest extends AbstractTestContainers {

    @Autowired
    private ReconciliationJob job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreateAccountPort createAccountPort;

    @Autowired
    private DepositPort depositPort;

    @BeforeEach
    void clean() {
        // The detection query scans every account, so the test must start from
        // an empty database regardless of what other test classes left behind.
        jdbcTemplate.update("DELETE FROM reconciliation_discrepancies");
        jdbcTemplate.update("DELETE FROM ledger_entries");
        jdbcTemplate.update("DELETE FROM movements");
        jdbcTemplate.update("DELETE FROM transactions");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    @DisplayName("a consistent database reports no discrepancies")
    void consistentDatabaseHasNoDiscrepancies() {
        // Accounts are created at zero and funded with a deposit: the ledger is
        // the source of truth, so an opening balance without a movement would
        // be reported as a discrepancy (and rightly so).
        Account first = createAccountPort.execute("recon-one@bank.com", BigDecimal.ZERO);
        depositPort.deposit(first.getIban(), new BigDecimal("100"), "recon-one@bank.com");
        Account second = createAccountPort.execute("recon-two@bank.com", BigDecimal.ZERO);
        depositPort.deposit(second.getIban(), new BigDecimal("50"), "recon-two@bank.com");

        int open = job.reconcile();

        assertThat(open).isZero();
        assertThat(countOpen()).isZero();
        assertThat(totalRows()).isZero();
        assertThat(ledgerNet()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a corrupted balance is reported once and never repaired")
    void corruptedBalanceIsReportedOnce() {
        Account account = createAccountPort.execute("recon-three@bank.com", BigDecimal.ZERO);
        depositPort.deposit(account.getIban(), new BigDecimal("100"), "recon-three@bank.com");
        corruptBalance(account.getIban(), new BigDecimal("101"));

        assertThat(job.reconcile()).isEqualTo(1);

        Map<String, Object> row = openRow();
        assertThat(row.get("iban")).isEqualTo(account.getIban());
        assertThat((BigDecimal) row.get("balance")).isEqualByComparingTo("101");
        assertThat((BigDecimal) row.get("ledger_net")).isEqualByComparingTo("100");
        assertThat((BigDecimal) row.get("difference")).isEqualByComparingTo("1");

        // The job reports; it does not fix.
        assertThat(balanceOf(account.getIban())).isEqualByComparingTo("101");

        // Second run with the same problem: one open row, not two.
        assertThat(job.reconcile()).isEqualTo(1);
        assertThat(totalRows()).isEqualTo(1);
        assertThat(countOpen()).isEqualTo(1);
    }

    @Test
    @DisplayName("a fixed balance resolves the open discrepancy")
    void fixedBalanceResolvesTheDiscrepancy() {
        Account account = createAccountPort.execute("recon-four@bank.com", BigDecimal.ZERO);
        depositPort.deposit(account.getIban(), new BigDecimal("100"), "recon-four@bank.com");
        corruptBalance(account.getIban(), new BigDecimal("101"));
        assertThat(job.reconcile()).isEqualTo(1);

        corruptBalance(account.getIban(), new BigDecimal("100"));
        assertThat(job.reconcile()).isZero();

        assertThat(countOpen()).isZero();
        assertThat(totalRows()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT resolved_at FROM reconciliation_discrepancies", Object.class)).isNotNull();

        // A third run must not create a new row.
        assertThat(job.reconcile()).isZero();
        assertThat(totalRows()).isEqualTo(1);
    }

    private void corruptBalance(String iban, BigDecimal newBalance) {
        jdbcTemplate.update("UPDATE accounts SET balance = ? WHERE iban = ?", newBalance, iban);
    }

    private BigDecimal balanceOf(String iban) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE iban = ?", BigDecimal.class, iban);
    }

    private BigDecimal ledgerNet() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN direction = 'C' THEN amount ELSE -amount END), 0)
                FROM ledger_entries
                """, BigDecimal.class);
    }

    private Map<String, Object> openRow() {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM reconciliation_discrepancies WHERE resolved_at IS NULL");
    }

    private int countOpen() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reconciliation_discrepancies WHERE resolved_at IS NULL", Integer.class);
        return count == null ? 0 : count;
    }

    private int totalRows() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reconciliation_discrepancies", Integer.class);
        return count == null ? 0 : count;
    }
}
