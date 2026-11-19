package com.banking.accounts.infrastructure.config;

import com.banking.accounts.application.usecase.CreateAccountPort;
import com.banking.accounts.application.usecase.CreateAccountUseCase;
import com.banking.accounts.application.usecase.DeleteAccountPort;
import com.banking.accounts.application.usecase.DeleteAccountUseCase;
import com.banking.accounts.application.usecase.GetAccountsPort;
import com.banking.accounts.application.usecase.GetAccountsUseCase;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import com.banking.ledger.domain.repository.MovementRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class AccountUseCaseConfig {

    @Bean
    public CreateAccountPort createAccountPort(AccountRepository repo,
                                               MovementRepository movementRepository,
                                               LedgerEntryRepository ledgerEntryRepository,
                                               TransactionTemplate transactionTemplate) {
        return new CreateAccountUseCase(repo, movementRepository, ledgerEntryRepository, transactionTemplate);
    }

    @Bean
    public GetAccountsPort getAccountsPort(AccountRepository repo) {
        return new GetAccountsUseCase(repo);
    }

    @Bean
    public DeleteAccountPort deleteAccountPort(AccountRepository repo) {
        return new DeleteAccountUseCase(repo);
    }
}