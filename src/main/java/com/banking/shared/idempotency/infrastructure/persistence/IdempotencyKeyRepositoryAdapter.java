package com.banking.shared.idempotency.infrastructure.persistence;

import com.banking.shared.idempotency.domain.exception.DuplicateIdempotencyKeyException;
import com.banking.shared.idempotency.domain.model.IdempotencyKey;
import com.banking.shared.idempotency.domain.model.IdempotencyStatus;
import com.banking.shared.idempotency.domain.repository.IdempotencyKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Persists idempotency keys.
 *
 * The unique constraint (principal, scope, idempotency_key) is the concurrency
 * control: the database, not the application, decides which request wins. A
 * duplicate insert is translated into DuplicateIdempotencyKeyException so the
 * template can replay the stored response instead of executing twice.
 */
@Repository
public class IdempotencyKeyRepositoryAdapter implements IdempotencyKeyRepository {

    // PostgreSQL SQLSTATE for unique_violation (class 23 = integrity constraint).
    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    private final SpringDataIdempotencyKeyRepository repo;
    private final IdempotencyKeyMapper mapper;

    public IdempotencyKeyRepositoryAdapter(SpringDataIdempotencyKeyRepository repo,
                                           IdempotencyKeyMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public IdempotencyKey insert(IdempotencyKey key) {
        try {
            IdempotencyKeyEntity saved = repo.saveAndFlush(mapper.toEntity(key));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw new DuplicateIdempotencyKeyException(
                        "Idempotency key already exists for principal=%s, scope=%s, key=%s"
                                .formatted(key.principal(), key.scope(), key.idempotencyKey())
                );
            }
            throw e;
        }
    }

    @Override
    public Optional<IdempotencyKey> find(String principal, String scope, String idempotencyKey) {
        return repo.findByPrincipalAndScopeAndIdempotencyKey(principal, scope, idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public void complete(Long id, String responseBody, LocalDateTime completedAt) {
        IdempotencyKeyEntity entity = repo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency key not found: " + id));
        entity.setStatus(IdempotencyStatus.COMPLETED);
        entity.setResponseBody(responseBody);
        entity.setCompletedAt(completedAt);
    }

    @Override
    public int deleteExpired() {
        return repo.deleteExpired();
    }

    
    private boolean isUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause instanceof SQLException sqlException
                && UNIQUE_VIOLATION_SQLSTATE.equals(sqlException.getSQLState());
    }
}
