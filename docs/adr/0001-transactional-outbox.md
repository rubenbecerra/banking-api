# ADR 0001 — Transactional outbox with a leased relay

- **Status:** Accepted
- **Date:** 2026-11-19
- **Context:** P3 (outbox)
- **Supersedes:** none

## Context

After a transfer commits, other things have to happen: notify the receiver, feed
analytics, invalidate a cache, flag suspicious movements. Publishing to a broker
right after the commit is a **dual write**: two systems inside one atomicity
boundary that neither owns.

Both naive orders are wrong:

- **Publish inside the transaction.** A rollback after publishing leaves
  consumers with a payment that never happened.
- **Publish after the commit.** A crash between commit and publish loses the
  event although the money moved.

Neither failure is acceptable for money, and neither can be fixed by retrying
harder: the information needed (did the publish happen?) is in the process that
crashed.

## Decision

Write the event to an `outbox` table **in the same transaction** as the movement
and its two ledger entries. A separate relay polls pending rows, claims one with
a lease, publishes it and only then marks it as published.

- Money and event commit or roll back together.
- The relay query is supported by a partial index (`WHERE published_at IS NULL`),
  so published rows cost nothing while they are kept.
- Claim is a conditional `UPDATE`; a second relay cannot publish the same row at
  the same time. The lease expires, so a crashed relay does not block a row.
- Delivery is **at-least-once** by design. A crash between publish and mark
  produces a duplicate. Consumers deduplicate by `outbox.id`, which is stable
  across retries.
- Published rows are audit-only and are purged after a retention window
  (7 days by default), never pending ones.

## Consequences

### Positive

- No dual write: the atomicity boundary is the database transaction.
- No broker required to be correct; the publisher is a port (today: a log).
- Multiple relay instances are safe; the lease serialises them per row.
- The delivery contract is explicit and tested: one row per committed transfer,
  duplicates possible, dedup at the consumer.

### Negative / costs

- **At-least-once, not exactly-once.** Any consumer must be idempotent. This is
  not a bug to fix; it is impossible to eliminate in a DB-to-broker bridge.
- One extra insert per money movement, and up to `interval-ms` of added latency.
- The table grows and needs a purge job; with millions of rows the single
  `DELETE` should become batched.
- The relay polls; a change-data-capture setup would have lower latency at the
  cost of much more infrastructure.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Publish inside the transaction | Consumers see events for money that rolled back |
| Publish after commit, retry in memory | Event lost on crash; no durable record |
| CDC (Debezium) on the ledger tables | Correct and low-latency, but a new cluster to operate. Overkill here; the outbox shares the same guarantee with one table |
| Two-phase commit / XA | Poor support, high complexity, still does not solve the broker ACK problem |

## References

- `V7__Create_Outbox_Table.sql`
- `com.banking.shared.outbox` (domain, relay, purge, adapters)
- `OutboxProducerIntegrationTest`, `OutboxRelayIntegrationTest`,
  `OutboxRepositoryIntegrationTest`, `OutboxPurgeJobTest`
