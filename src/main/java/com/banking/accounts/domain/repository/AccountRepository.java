package com.banking.accounts.domain.repository;

import com.banking.accounts.domain.model.Account;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByIbanWithLock(String iban);
    Optional<Account> findByIban(String iban);
    List<Account> findAllByOwner(String owner);
    List<Account> findAll();
    boolean existsByIban(String iban);
    Account save(Account account);
    void delete(Account account);
    void deleteAll();
}