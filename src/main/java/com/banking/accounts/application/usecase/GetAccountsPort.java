package com.banking.accounts.application.usecase;

import com.banking.accounts.domain.model.Account;

import java.util.List;

public interface GetAccountsPort {
    List<Account> getByOwner(String owner);
    List<Account> getAll();
}
