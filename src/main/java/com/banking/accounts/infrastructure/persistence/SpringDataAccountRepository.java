package com.banking.accounts.infrastructure.persistence;

import com.banking.accounts.domain.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataAccountRepository extends JpaRepository<AccountEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.iban = :iban")
    Optional<AccountEntity> findByIbanWithLock(@Param("iban") String iban);
    Optional<AccountEntity> findByIban(String iban);
    List<AccountEntity> findAllByOwner(String owner);
    boolean existsByIban(String iban);
}
