package com.banking.shared.outbox.infrastructure.persistence;

import com.banking.shared.outbox.domain.exception.OutboxEventAlreadyPersistedException;
import com.banking.shared.outbox.domain.model.OutboxEvent;
import com.banking.shared.outbox.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA adapter for the outbox port.
 *
 * {@link #append(OutboxEvent)} runs in the caller's transaction on purpose: the
 * event must commit or roll back with the money. The three write operations get
 * their own transaction because the relay calls them with none open, and a
 * {@code @Modifying} query needs an active transaction.
 */
@Repository
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final SpringDataOutboxEventRepository repository;
    private final OutboxEventMapper mapper;

    public OutboxEventRepositoryAdapter(SpringDataOutboxEventRepository repository,
                                        OutboxEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OutboxEvent append(OutboxEvent event) {
        if (event.id() != null) {
            throw new OutboxEventAlreadyPersistedException(
                    "Outbox event is already persisted: " + event.id());
        }
        OutboxEventEntity saved = repository.saveAndFlush(mapper.toEntity(event));
        return mapper.toDomain(saved);
    }

    @Override
    public List<OutboxEvent> findPending(int limit, LocalDateTime claimCutoff) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return repository.findPending(claimCutoff, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean claim(long id, LocalDateTime claimedAt, LocalDateTime staleCutoff) {
        return repository.claim(id, claimedAt, staleCutoff) == 1;
    }

    @Override
    @Transactional
    public boolean markPublished(long id, LocalDateTime publishedAt) {
        return repository.markPublished(id, publishedAt) == 1;
    }

    @Override
    @Transactional
    public int deletePublishedBefore(LocalDateTime cutoff) {
        return repository.deletePublishedBefore(cutoff);
    }
}
