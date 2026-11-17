package com.banking.shared.outbox.domain.model;

/**
 * Values for {@code outbox.aggregate_type}.
 *
 * <p>Constants instead of an enum: the value is a contract with external
 * consumers and travels as text through the port. An enum would only add
 * {@code name()}/{@code valueOf()} conversions without preventing a typo in a
 * string that is not validated by the database.
 */
public final class OutboxAggregateTypes {

    public static final String MOVEMENT = "MOVEMENT";

    private OutboxAggregateTypes() {
    }
}
