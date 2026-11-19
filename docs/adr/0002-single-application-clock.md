# ADR 0002 — One application clock for everything that measures time

- **Status:** Accepted
- **Date:** 2026-11-19
- **Context:** idempotency purge, outbox relay, outbox purge

## Context

Three features read "now": the idempotency window (24 h), the relay lease (30 s)
and the two purge jobs. The readings were spread over two clocks:

- `expires_at` was written by the application (`LocalDateTime.now()`), but the
  purge compared it against `CURRENT_TIMESTAMP`, the **database** clock.
- `published_at` is written by the relay with the JVM clock; the new outbox purge
  would have needed to compare it against the same clock to be consistent.

Two clocks and one value only agree while both machines agree. In containers and
across time zones they usually do, until they don't — and the bug appears as a
key purged 30 seconds early or a row that survives one extra day, which is
almost impossible to reproduce.

## Decision

One `Clock` bean (`com.banking.shared.config.TimeConfig`) is injected wherever the
application reads "now": the idempotency template, the relay and both purge jobs.

- Ports take the **cutoff from the caller** (`deleteExpiredBefore(cutoff)`,
  `deletePublishedBefore(cutoff)`), so the adapter never decides what "now" is
  and can be tested with fixed instants.
- Whoever writes a timestamp and whoever compares it use the same clock.
- Database time is still used for column defaults as a safety net, never as a
  comparison value.

## Consequences

### Positive

- Writer and reader of a timestamp can no longer disagree.
- Tests can override one bean and control time everywhere at once.
- The ports are explicit about the cutoff: an interview answer is one sentence,
  not a hunt through SQL.

### Negative / costs

- Slightly more wiring (`TimeConfig`, constructor parameters).
- Domain factories (`Movement`, `LedgerEntry`, `Transaction`) still call
  `LocalDateTime.now()`. Moving them to an injected clock is a domain change and
  is left as a known debt; it cannot introduce skew today because they run in the
  same process as the `Clock` bean.
- The scheduler pool grew to three threads; the property comment states why.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Database clock everywhere | Would require writers to use database time too, through SQL instead of domain objects |
| A clock per component | Reproduces the original problem with more code |
| `LocalDateTime.now()` in adapters only | Adapters do not decide business time; the caller does |

## References

- `com.banking.shared.config.TimeConfig`
- `IdempotencyTemplate`, `IdempotencyKeyPurgeJob`, `OutboxPurgeJob`
- `IdempotencyKeyPurgeJobTest`, `OutboxPurgeJobTest`
