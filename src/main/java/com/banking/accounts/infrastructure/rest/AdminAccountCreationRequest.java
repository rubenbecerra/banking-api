package com.banking.accounts.infrastructure.rest;

import java.math.BigDecimal;

public record AdminAccountCreationRequest(
        String ownerEmail,
        BigDecimal initialBalance
) {
}
