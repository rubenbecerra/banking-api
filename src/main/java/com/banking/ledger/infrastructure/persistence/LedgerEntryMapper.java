package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.domain.model.LedgerEntry;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryMapper {

    public LedgerEntry toDomain(LedgerEntryEntity entity) {
        if (entity == null) {
            return null;
        }

        return new LedgerEntry(
                entity.getId(),
                entity.getMovementId(),
                entity.getAccountIban(),
                entity.getDirection(),
                entity.getAmount(),
                entity.getCreatedAt()
        );
    }

    public LedgerEntryEntity toEntity(LedgerEntry domain) {
        if (domain == null) {
            return null;
        }
        LedgerEntryEntity entity = new LedgerEntryEntity();
        entity.setId(domain.getId());
        entity.setMovementId(domain.getMovementId());
        entity.setAccountIban(domain.getAccountIban());
        entity.setDirection(domain.getDirection());
        entity.setAmount(domain.getAmount());
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }
}
