package com.banking.accounts.application.usecase;

import com.banking.accounts.domain.exception.AccountNotFoundException;
import com.banking.accounts.domain.exception.UnauthorizedActionException;
import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;

public class DeleteAccountUseCase implements DeleteAccountPort {
    private final AccountRepository repo;

    public DeleteAccountUseCase(AccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public void execute(String owner, String iban) {
        Account account = repo.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account with IBAN [%s] not found".formatted(iban)));

        if (!account.getOwner().equals(owner)) {
            throw new UnauthorizedActionException("You do not have permission to delete this account");
        }
        repo.delete(account);
    }

    @Override
    public void executeByAdmin(String iban) {
        Account account = repo.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account with IBAN [%s] not found".formatted(iban)));
        repo.delete(account);
    }
}