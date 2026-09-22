package com.banking.transactions.infrastructure.persistence;

import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapper {

    public Transaction toDomain(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Transaction(
                entity.getId(),
                entity.getSourceIban(),
                entity.getTargetIban(),
                entity.getAmount(),
                entity.getType(),
                entity.getTimestamp()
        );
    }

    public TransactionEntity toEntity(Transaction domain) {
        if (domain == null) {
            return null;
        }
        TransactionEntity entity = new TransactionEntity();
        entity.setId(domain.getId());
        entity.setSourceIban(domain.getSourceIban());
        entity.setTargetIban(domain.getTargetIban());
        entity.setAmount(domain.getAmount());
        entity.setType(domain.getType());
        entity.setTimestamp(domain.getTimestamp());

        return entity;
    }

    public PageResult<Transaction> toDomainPage(Page<TransactionEntity> entityPage) {
        if (entityPage == null) {
            return null;
        }
        List<Transaction> content = entityPage.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PageResult<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast()
        );
    }
}