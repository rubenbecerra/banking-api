package com.banking.transactions.infrastructure.rest;

import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionRestMapper {

    public TransactionDTO toDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionDTO(
                transaction.getId(),
                transaction.getSourceIban(),
                transaction.getTargetIban(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getTimestamp()
        );
    }
    public PageResult<TransactionDTO> toPageDTO(PageResult<Transaction> domainPage) {
        if (domainPage == null) {
            return null;
        }
        List<TransactionDTO> dtoList = domainPage.content().stream()
                .map(this::toDTO)
                .toList();

        return new PageResult<>(
                dtoList,
                domainPage.number(),
                domainPage.pageSize(),
                domainPage.totalElements(),
                domainPage.totalPages(),
                domainPage.last()
        );
    }
}