package com.banking.transactions.domain.repository;

import com.banking.transactions.domain.model.PageQuery;
import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;

import java.util.List;


public interface TransactionRepository {
    PageResult<Transaction> findBySourceIbanOrTargetIban(
            String sourceIban,
            String targetIban,
            PageQuery pageQuery
    );
    Transaction save(Transaction transaction);
    void deleteAll();
    List<Transaction> findAll();
    long count();
}
