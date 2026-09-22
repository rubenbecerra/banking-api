package com.banking.transactions.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Page<TransactionEntity> findBySourceIbanOrTargetIban(
            String sourceIban,
            String targetIban,
            Pageable pageable
    );
}
