package com.banking.accounts.application.usecase;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.util.IbanGenerator;
import java.math.BigDecimal;

public class CreateAccountUseCase implements CreateAccountPort {
    private final AccountRepository repo;

    public CreateAccountUseCase(AccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public Account execute(String ownerEmail, BigDecimal balance) {
        String generatedIban = IbanGenerator.generateSpanishIban();
        Account account = new Account(generatedIban, balance, ownerEmail);
        return repo.save(account);
    }
}