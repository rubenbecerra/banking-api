package com.banking.transactions.infrastructure.rest;

import com.banking.transactions.domain.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDTO(
        Long id,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        TransactionType type,
        LocalDateTime timestamp
) {}
