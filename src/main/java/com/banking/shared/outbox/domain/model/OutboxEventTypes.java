package com.banking.shared.outbox.domain.model;

/**
 * Values for {@code outbox.event_type}.
 *
 * <p>Past-tense, PascalCase: an event states a fact that already happened, it
 * does not request an action. {@code MoneyTransferred}, never
 * {@code NotifyReceiver}.
 */
public final class OutboxEventTypes {

    public static final String MONEY_TRANSFERRED = "MoneyTransferred";

    private OutboxEventTypes() {
    }
}
