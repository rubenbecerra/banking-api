package com.banking.transactions.infrastructure.persistence;

import com.banking.transactions.domain.model.PageQuery;
import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository transactionRepository;
    private final TransactionMapper mapper;

    public TransactionRepositoryAdapter(SpringDataTransactionRepository transactionRepository, TransactionMapper mapper) {
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    @Override
    public PageResult<Transaction> findBySourceIbanOrTargetIban(String sourceIban, String targetIban, PageQuery pageQuery) {
        Sort sort = Sort.by(Sort.Direction.DESC, pageQuery.sortBy() != null ? pageQuery.sortBy() : "timestamp");
        Pageable pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), sort);

        Page<TransactionEntity> entityPage = transactionRepository.findBySourceIbanOrTargetIban(sourceIban, targetIban, pageable);

        return mapper.toDomainPage(entityPage);
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = mapper.toEntity(transaction);
        TransactionEntity savedEntity = transactionRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void deleteAll() {
        transactionRepository.deleteAll();
    }

    @Override
    public List<Transaction> findAll() {
        return transactionRepository.findAll().stream().map(mapper::toDomain).toList();
    }
    @Override
    public long count() {
        return transactionRepository.count();
    }
}