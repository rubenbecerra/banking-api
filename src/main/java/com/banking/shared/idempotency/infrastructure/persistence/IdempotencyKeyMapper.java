package com.banking.shared.idempotency.infrastructure.persistence;

import com.banking.shared.idempotency.domain.model.IdempotencyKey;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyMapper {

    public IdempotencyKey toDomain(IdempotencyKeyEntity entity) {
        if (entity == null) {
            return null;
        }
        return new IdempotencyKey(
                entity.getId(),
                entity.getPrincipal(),
                entity.getScope(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getResponseBody(),
                entity.getRequestFingerprint(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getExpiresAt()
        );
    }

    public IdempotencyKeyEntity toEntity(IdempotencyKey key) {
        if (key == null) {
            return null;
        }
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setId(key.id());
        entity.setPrincipal(key.principal());
        entity.setScope(key.scope());
        entity.setIdempotencyKey(key.idempotencyKey());
        entity.setStatus(key.status());
        entity.setResponseBody(key.responseBody());
        entity.setRequestFingerprint(key.requestFingerprint());
        entity.setCreatedAt(key.createdAt());
        entity.setCompletedAt(key.completedAt());
        entity.setExpiresAt(key.expiresAt());
        return entity;
    }
}
