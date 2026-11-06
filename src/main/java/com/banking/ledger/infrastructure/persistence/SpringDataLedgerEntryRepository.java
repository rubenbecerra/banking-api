package com.banking.ledger.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataLedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {
    List<LedgerEntryEntity> findByMovementId(Long movementId);
}
