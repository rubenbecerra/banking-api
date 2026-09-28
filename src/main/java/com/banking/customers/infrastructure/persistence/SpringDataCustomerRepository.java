package com.banking.customers.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringDataCustomerRepository extends JpaRepository<CustomerJpaEntity, Integer> {
    Optional<CustomerJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}