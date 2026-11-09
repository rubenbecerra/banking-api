package com.banking.shared.idempotency.domain.repository;

import com.banking.shared.idempotency.domain.model.IdempotencyKey;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyKeyRepository {
    IdempotencyKey insert(IdempotencyKey key);
    Optional<IdempotencyKey> find(String principal, String scope, String idempotencyKey);
    void complete(Long id, String responseBody, LocalDateTime completedAt);
    int deleteExpired();
}
