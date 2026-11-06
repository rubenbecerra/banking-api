-- Deposits and withdrawals use 'EXTERNAL' as the counterparty account in ledger_entries.
--
-- Double-entry ledger:
--   * movements       -> one row per business operation (transfer/deposit/withdrawal)
--   * ledger_entries  -> two rows per movement: one debit (D) and one credit (C)
--
-- Transitional design:
--   * `transactions` remains as the existing read/history model.
--   * `movements` + `ledger_entries` are the new ledger.
--   * The use cases will write both inside the same database transaction.

CREATE TABLE movements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(25) NOT NULL,
    source_iban VARCHAR(24),
    target_iban VARCHAR(24),
    amount NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Only the three supported operation types.
    CONSTRAINT chk_movements_type
        CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL')),

    -- The amount is always positive; the type defines the direction.
    CONSTRAINT chk_movements_amount
        CHECK (amount > 0),

    -- Each type requires a different combination of accounts.
    -- TRANSFER:   source + target
    -- DEPOSIT:    target only (source is external)
    -- WITHDRAWAL: source only (target is external)
    CONSTRAINT chk_movements_accounts
        CHECK (
            (type = 'TRANSFER'   AND source_iban IS NOT NULL AND target_iban IS NOT NULL) OR
            (type = 'DEPOSIT'    AND source_iban IS NULL     AND target_iban IS NOT NULL) OR
            (type = 'WITHDRAWAL' AND source_iban IS NOT NULL AND target_iban IS NULL)
        )
);

CREATE TABLE ledger_entries (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    movement_id BIGINT NOT NULL,
    account_iban VARCHAR(24) NOT NULL,
    direction CHAR(1) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_entries_movement
        FOREIGN KEY (movement_id) REFERENCES movements(id),

    -- A ledger entry is either a debit or a credit.
    CONSTRAINT chk_ledger_entries_direction
        CHECK (direction IN ('D', 'C')),

    CONSTRAINT chk_ledger_entries_amount
        CHECK (amount > 0),

    -- At most one debit and one credit per movement.
    -- The application invariant + tests guarantee exactly two entries.
    CONSTRAINT uq_ledger_entries_movement_direction
        UNIQUE (movement_id, direction)
);

-- Account history: filter by account and order by date in one index.
CREATE INDEX idx_ledger_entries_account_created
    ON ledger_entries(account_iban, created_at DESC);

-- Recent movements: global pagination by date.
CREATE INDEX idx_movements_created_at
    ON movements(created_at DESC);

-- No separate index on ledger_entries(movement_id):
-- the UNIQUE (movement_id, direction) index already supports WHERE movement_id = ?.
--
-- No index on type/direction/amount:
-- low cardinality or not a core query. Add only if EXPLAIN shows a need.
