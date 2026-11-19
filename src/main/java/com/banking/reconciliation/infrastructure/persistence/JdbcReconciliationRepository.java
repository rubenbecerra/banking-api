package com.banking.reconciliation.infrastructure.persistence;

import com.banking.reconciliation.domain.model.Discrepancy;
import com.banking.reconciliation.domain.repository.ReconciliationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Plain JDBC on purpose: this is a read model over two tables and a small
 * audit table, with PostgreSQL-specific SQL (the partial-index upsert). Mapping
 * it as a JPA entity would add a persistence context to a nightly batch job
 * without any benefit.
 *
 * The adapter joins the caller's transaction; the service owns it.
 */
@Repository
public class JdbcReconciliationRepository implements ReconciliationRepository {

    /**
     * One pass over accounts and entries. The LEFT JOIN keeps accounts with no
     * entries (their ledger net is zero), which is exactly what an account
     * created with a balance and no movement looks like.
     */
    private static final String DETECT_SQL = """
            SELECT a.id,
                   a.iban,
                   a.balance,
                   COALESCE(SUM(CASE e.direction WHEN 'C' THEN e.amount ELSE -e.amount END), 0) AS ledger_net
            FROM accounts a
            LEFT JOIN ledger_entries e ON e.account_iban = a.iban
            GROUP BY a.id, a.iban, a.balance
            HAVING a.balance <> COALESCE(SUM(CASE e.direction WHEN 'C' THEN e.amount ELSE -e.amount END), 0)
            ORDER BY a.id
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO reconciliation_discrepancies
                (account_id, iban, ledger_net, balance, difference, detected_at, last_seen_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (account_id) WHERE resolved_at IS NULL
            DO UPDATE SET ledger_net   = EXCLUDED.ledger_net,
                          balance      = EXCLUDED.balance,
                          difference   = EXCLUDED.difference,
                          last_seen_at = EXCLUDED.last_seen_at
            """;

    private static final String LEDGER_NET_SQL = """
            SELECT COALESCE(SUM(CASE WHEN direction = 'C' THEN amount ELSE -amount END), 0)
            FROM ledger_entries
            """;

    private final JdbcTemplate jdbc;

    public JdbcReconciliationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Discrepancy> findDiscrepancies() {
        return jdbc.query(DETECT_SQL, (rs, rowNum) -> new Discrepancy(
                rs.getLong("id"),
                rs.getString("iban"),
                rs.getBigDecimal("balance"),
                rs.getBigDecimal("ledger_net")));
    }

    @Override
    public void upsertOpen(List<Discrepancy> discrepancies, LocalDateTime seenAt) {
        if (discrepancies.isEmpty()) {
            return;
        }
        Timestamp seen = Timestamp.valueOf(seenAt);
        jdbc.batchUpdate(UPSERT_SQL, discrepancies, discrepancies.size(), (ps, d) -> {
            ps.setLong(1, d.accountId());
            ps.setString(2, d.iban());
            ps.setBigDecimal(3, d.ledgerNet());
            ps.setBigDecimal(4, d.balance());
            ps.setBigDecimal(5, d.difference());
            ps.setTimestamp(6, seen);
            ps.setTimestamp(7, seen);
        });
    }

    @Override
    public int resolveAllExcept(Collection<Long> stillDiscrepantAccountIds, LocalDateTime resolvedAt) {
        Timestamp resolved = Timestamp.valueOf(resolvedAt);
        if (stillDiscrepantAccountIds.isEmpty()) {
            return jdbc.update(
                    "UPDATE reconciliation_discrepancies SET resolved_at = ? WHERE resolved_at IS NULL",
                    resolved);
        }

        String placeholders = stillDiscrepantAccountIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] args = Stream.concat(
                        Stream.<Object>of(resolved),
                        stillDiscrepantAccountIds.stream().map(id -> (Object) id))
                .toArray();
        return jdbc.update("""
                UPDATE reconciliation_discrepancies
                SET resolved_at = ?
                WHERE resolved_at IS NULL
                  AND account_id NOT IN (%s)
                """.formatted(placeholders), args);
    }

    @Override
    public BigDecimal ledgerNet() {
        BigDecimal net = jdbc.queryForObject(LEDGER_NET_SQL, BigDecimal.class);
        return net == null ? BigDecimal.ZERO : net;
    }
}
