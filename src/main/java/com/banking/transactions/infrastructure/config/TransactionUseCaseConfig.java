package com.banking.transactions.infrastructure.config;

import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.transactions.application.decorator.DepositUseCaseTransactionalDecorator;
import com.banking.transactions.application.decorator.GetTransactionsUseCaseTransactionalDecorator;
import com.banking.transactions.application.decorator.TransferMoneyUseCaseTransactionalDecorator;
import com.banking.transactions.application.decorator.WithdrawUseCaseTransactionalDecorator;
import com.banking.transactions.application.usecase.*;
import com.banking.transactions.domain.repository.TransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionUseCaseConfig {

    @Bean
    public DepositPort depositPort(TransactionRepository txRepo, AccountRepository accRepo) {
        DepositUseCase useCase = new DepositUseCase(txRepo, accRepo);
        return new DepositUseCaseTransactionalDecorator(useCase);
    }

    @Bean
    public WithdrawPort withdrawPort(TransactionRepository txRepo, AccountRepository accRepo) {
        WithdrawUseCase useCase = new WithdrawUseCase(txRepo, accRepo);
        return new WithdrawUseCaseTransactionalDecorator(useCase);
    }

    @Bean
    public TransferMoneyPort transferMoneyPort(TransactionRepository txRepo, AccountRepository accRepo) {
        TransferMoneyUseCase useCase = new TransferMoneyUseCase(txRepo, accRepo);
        return new TransferMoneyUseCaseTransactionalDecorator(useCase);
    }

    @Bean
    public GetTransactionsPort getTransactionsPort(TransactionRepository txRepo, AccountRepository accRepo) {
        GetTransactionsUseCase useCase = new GetTransactionsUseCase(txRepo, accRepo);
        return new GetTransactionsUseCaseTransactionalDecorator(useCase);
    }
}