-- Transactional outbox for domain events.
--
-- Problem: after a transfer commits, other things must happen (notify the
-- receiver, update analytics, invalidate a cache, feed fraud detection). Calling
-- a broker right after the commit is a dual write: two systems, one atomicity
-- boundary. If the publish fails the event is lost; if the transaction rolls
-- back after a publish, consumers see a payment that never happened.
--
-- Solution: the event row is inserted in the SAME transaction as the movement
-- and its ledger entries. Money and event commit or roll back together.
--
-- Delivery is AT-LEAST-ONCE: a relay polls pending rows, claims one with a
-- lease, publishes it and only then marks it published. A crash between publish
-- and mark produces a duplicate, so consumers must deduplicate by event id.
--
-- The relay query is:
--   SELECT ... FROM outbox WHERE published_at IS NULL ORDER BY created_at LIMIT ?
-- hence the partial index below.

CREATE TABLE outbox
(
    -- Own identity, not the movement id: one movement may emit more than one
    -- event later, and consumers deduplicate by this id.
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Which aggregate the event belongs to. Today the only producer is the
    -- ledger (MOVEMENT); it is explicit so a second producer cannot be guessed.
    aggregate_type VARCHAR(32) NOT NULL,

    -- Id of that aggregate (movements.id today). Deliberately no foreign key:
    -- the outbox is append-only and must not depend on the lifecycle of the
    -- ledger tables. BIGINT because every current aggregate id is numeric;
    -- if one becomes UUID, convert this column to VARCHAR once, here.
    aggregate_id   BIGINT      NOT NULL,

    -- Contract with consumers. Bounded on purpose.
    event_type     VARCHAR(60) NOT NULL,

    -- The event itself. JSONB so it is validated on insert and can be inspected
    -- from psql when debugging; the relay treats it as opaque.
    payload        JSONB       NOT NULL,

    -- When the event was written. Set by the application (same as
    -- idempotency_keys), the default only protects manual inserts.
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Lease: who is publishing it and since when. Expires, so a crashed relay
    -- does not block the row forever.
    claimed_at     TIMESTAMP   NULL,

    -- Published at. NULL = pending: this column is the state, no status machine.
    published_at   TIMESTAMP   NULL,

    -- Nothing can be published without having been claimed first.
    CONSTRAINT chk_outbox_published_after_claim
        CHECK (published_at IS NULL OR claimed_at IS NOT NULL)
);

-- Partial index: only pending rows are indexed. Published rows (eventually the
-- vast majority) cost nothing to keep, and the index stays small.
CREATE INDEX idx_outbox_pending
    ON outbox (created_at)
    WHERE published_at IS NULL;

-- No index on aggregate_id: nothing queries "events of one movement" yet.
-- Add it only when a real query needs it.
