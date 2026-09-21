package com.banking.accounts.application.usecase;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import java.util.List;

public class GetAccountsUseCase implements GetAccountsPort {
    private final AccountRepository repo;

    public GetAccountsUseCase(AccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Account> getByOwner(String owner) {
        return repo.findAllByOwner(owner);
    }

    @Override
    public List<Account> getAll() {
        return repo.findAll();
    }
}
