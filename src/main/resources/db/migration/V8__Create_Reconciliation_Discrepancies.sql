-- Reconciliation: detects accounts whose stored balance does not match the ledger.
--
-- Invariant (per account):  accounts.balance = sum(credits) - sum(debits)
-- from ledger_entries. A transfer debits the source and credits the target;
-- a deposit debits EXTERNAL and credits the target; a withdrawal debits the
-- source and credits EXTERNAL.
--
-- The job only REPORTS. It never repairs a balance: an automatic fix would
-- destroy the evidence of how the money went missing.
--
-- One open row per account: a discrepancy that persists for a week is one
-- problem, not seven. The partial unique index makes the upsert possible and
-- keeps the history (detected_at / resolved_at) in the same row.

CREATE TABLE reconciliation_discrepancies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- No FK on purpose: deleting an account must not fail because an old
    -- discrepancy still points at it. If the account disappears, the next run
    -- stops seeing it and resolves the row.
    account_id BIGINT NOT NULL,

    -- Denormalised for reading without a join; the account may be gone.
    iban VARCHAR(24) NOT NULL,

    -- Ledger total at detection time: sum(credits) - sum(debits).
    ledger_net NUMERIC(19,4) NOT NULL,

    -- accounts.balance at detection time.
    balance NUMERIC(19,4) NOT NULL,

    -- balance - ledger_net. Zero means there is no discrepancy.
    difference NUMERIC(19,4) NOT NULL,

    detected_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,

    -- A zero difference is not a discrepancy: if the detection query is ever
    -- wrong, this constraint turns a silent false positive into a failed job.
    CONSTRAINT chk_reconciliation_difference_not_zero
        CHECK (difference <> 0)
);

-- At most one open discrepancy per account. Resolved rows are history and do
-- not conflict, so the same account can break, be fixed and break again.
CREATE UNIQUE INDEX uq_reconciliation_open_per_account
    ON reconciliation_discrepancies (account_id)
    WHERE resolved_at IS NULL;
