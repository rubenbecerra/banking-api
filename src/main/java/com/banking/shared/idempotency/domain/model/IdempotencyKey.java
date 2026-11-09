package com.banking.shared.idempotency.domain.model;

import java.time.LocalDateTime;

public record IdempotencyKey(
        Long id,
        String principal,
        String scope,
        String idempotencyKey,
        IdempotencyStatus status,
        String responseBody,
        String requestFingerprint,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt
) {}
