package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.domain.model.LedgerEntry;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LedgerEntryRepositoryAdapter implements LedgerEntryRepository {

    private final SpringDataLedgerEntryRepository repo;
    private final LedgerEntryMapper mapper;

    public LedgerEntryRepositoryAdapter(SpringDataLedgerEntryRepository springData, LedgerEntryMapper mapper) {
        this.repo = springData;
        this.mapper = mapper;
    }

    @Override
    public List<LedgerEntry> saveAll(List<LedgerEntry> entries) {
        List<LedgerEntryEntity> entities = entries.stream().map(mapper::toEntity).toList();
        return repo.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<LedgerEntry> findByMovementId(Long movementId) {
        return repo.findByMovementId(movementId).stream().map(mapper::toDomain).toList();
    }
}
