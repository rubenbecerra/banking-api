package com.banking.transactions.domain.repository;

import com.banking.transactions.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySourceIbanOrTargetIbanOrderByTimestampDesc(
            String sourceIban,
            String targetIban,
            Pageable pageable
    );
}
