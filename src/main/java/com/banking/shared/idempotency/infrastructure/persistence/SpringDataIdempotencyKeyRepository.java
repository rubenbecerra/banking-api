package com.banking.shared.idempotency.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SpringDataIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {
    Optional<IdempotencyKeyEntity> findByPrincipalAndScopeAndIdempotencyKey(
            String principal, String scope, String idempotencyKey);

    @Modifying
    @Query("DELETE FROM IdempotencyKeyEntity e WHERE e.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpired();
}
