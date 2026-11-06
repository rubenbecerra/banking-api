package com.banking.ledger.domain.repository;


import com.banking.ledger.domain.model.LedgerEntry;

import java.util.List;

public interface LedgerEntryRepository {
    List<LedgerEntry> saveAll(List<LedgerEntry> entries);
    List<LedgerEntry> findByMovementId(Long movementId);
}
