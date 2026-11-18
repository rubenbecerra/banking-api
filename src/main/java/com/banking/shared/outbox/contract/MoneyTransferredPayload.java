package com.banking.shared.outbox.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MoneyTransferredPayload(
        long movementId,
        String fromIban,
        String toIban,
        BigDecimal amount,
        LocalDateTime occurredAt
) {}