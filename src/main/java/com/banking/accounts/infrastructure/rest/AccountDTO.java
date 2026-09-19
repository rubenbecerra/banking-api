package com.banking.accounts.infrastructure.rest;

import java.math.BigDecimal;

public record AccountDTO(

        String iban,
        BigDecimal balance,
        String owner
) {}
