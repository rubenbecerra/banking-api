-- Database-backed idempotency for money-moving operations.
--
-- One row per (principal, scope, idempotency_key). The row is inserted in the
-- same database transaction as the movement and its two ledger entries, so the
-- key and the money commit or roll back together. A retry that finds a
-- COMPLETED row returns the stored response without re-executing the operation.
--
-- A committed row is always COMPLETED: PROCESSING only exists inside the
-- uncommitted transaction that is executing the operation. If that transaction
-- rolls back, the row disappears with it.
--
-- Retention is 24 hours (expires_at). A scheduled job deletes expired rows, so
-- after that window the same key can be used again.
--
-- Replaces the previous Redis-based idempotency, which was not atomic with the
-- database commit.

CREATE TABLE idempotency_keys
(
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    principal       VARCHAR(255) NOT NULL,
    scope           VARCHAR(32)  NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PROCESSING',
    response_body   JSONB,
    request_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),

    CONSTRAINT uq_idempotency_keys_principal_scope_key
        UNIQUE(principal, scope, idempotency_key),

    CONSTRAINT chk_idempotency_keys_status
        CHECK (status IN ('PROCESSING', 'COMPLETED'))
);

-- Purge: DELETE FROM idempotency_keys WHERE expires_at < CURRENT_TIMESTAMP.
CREATE INDEX idx_idempotency_keys_expires_at
    ON idempotency_keys (expires_at);
