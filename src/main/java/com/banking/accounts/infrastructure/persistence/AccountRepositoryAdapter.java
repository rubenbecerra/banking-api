package com.banking.accounts.infrastructure.persistence;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final SpringDataAccountRepository jpaRepository;

    public AccountRepositoryAdapter(SpringDataAccountRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Account> findByIbanWithLock(String iban) {
        return jpaRepository.findByIbanWithLock(iban).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByIban(String iban) {
        return jpaRepository.findByIban(iban).map(this::toDomain);
    }

    @Override
    public List<Account> findAllByOwner(String owner) {
        return jpaRepository.findAllByOwner(owner).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Account> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByIban(String iban) {
        return jpaRepository.existsByIban(iban);
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = toEntity(account);
        AccountEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public void delete(Account account) {
        jpaRepository.delete(toEntity(account));
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    private Account toDomain(AccountEntity entity) {
        return new Account(
                entity.getId(),
                entity.getIban(),
                entity.getBalance(),
                entity.getOwner()
        );
    }

    private AccountEntity toEntity(Account account) {
        return new AccountEntity(
                account.getId(),
                account.getIban(),
                account.getBalance(),
                account.getOwner()
        );
    }
}