package com.banking.shared.outbox.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query(value = "SELECT * FROM outbox WHERE published_at IS NULL" +
            " AND (claimed_at IS NULL OR claimed_at < :cutoff) ORDER BY created_at LIMIT :limit", nativeQuery = true)
    List<OutboxEventEntity> findPending(@Param("cutoff") LocalDateTime cutoff,
                                        @Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE outbox SET claimed_at = :claimedAt " +
            "WHERE id = :id AND published_at IS NULL AND (claimed_at IS NULL OR claimed_at < :staleCutoff)",
            nativeQuery = true)
    int claim(@Param("id") long id,
              @Param("claimedAt") LocalDateTime claimedAt,
              @Param("staleCutoff") LocalDateTime staleCutoff);

    @Modifying
    @Query(value = "UPDATE outbox SET published_at = :publishedAt " +
            "WHERE id = :id AND published_at IS NULL", nativeQuery = true)
    int markPublished(@Param("id") long id,
                      @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Query(value = "DELETE FROM outbox WHERE published_at IS NOT NULL AND published_at < :cutoff",
            nativeQuery = true)
    int deletePublishedBefore(@Param("cutoff") LocalDateTime cutoff);
}
