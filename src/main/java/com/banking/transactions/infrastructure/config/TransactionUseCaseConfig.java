package com.banking.transactions.infrastructure.config;

import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.infrastructure.idempotency.IdempotencyTemplate;
import com.banking.shared.infrastructure.redis.IdempotencyRepository;
import com.banking.transactions.application.decorator.idempotency.DepositUseCaseIdempotencyDecorator;
import com.banking.transactions.application.decorator.idempotency.TransferUseCaseIdempotencyDecorator;
import com.banking.transactions.application.decorator.idempotency.WithdrawUseCaseIdempotencyDecorator;
import com.banking.transactions.application.decorator.transactional.DepositUseCaseTransactionalDecorator;
import com.banking.transactions.application.decorator.transactional.GetTransactionsUseCaseTransactionalDecorator;
import com.banking.transactions.application.decorator.transactional.TransferMoneyUseCaseTransactionalDecorator;
import com.banking.transactions.application.decorator.transactional.WithdrawUseCaseTransactionalDecorator;
import com.banking.transactions.application.usecase.*;
import com.banking.transactions.domain.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionUseCaseConfig {

    @Bean
    public IdempotencyTemplate idempotencyTemplate(IdempotencyRepository idempotencyRepository,
                                                   ObjectMapper objectMapper) {
        return new IdempotencyTemplate(idempotencyRepository, objectMapper);
    }

    @Bean
    public DepositPort depositPort(TransactionRepository txRepo,
                                   AccountRepository accRepo,
                                   IdempotencyTemplate idempotencyTemplate,
                                   TransactionTemplate transactionTemplate) {

        DepositUseCase useCase = new DepositUseCase(txRepo, accRepo);

        DepositUseCaseTransactionalDecorator transactionalDecorator =
                new DepositUseCaseTransactionalDecorator(useCase, transactionTemplate);

        return new DepositUseCaseIdempotencyDecorator(transactionalDecorator, idempotencyTemplate);
    }

    @Bean
    public WithdrawPort withdrawPort(TransactionRepository txRepo,
                                     AccountRepository accRepo,
                                     TransactionTemplate transactionTemplate,
                                     IdempotencyTemplate idempotencyTemplate) {

        WithdrawUseCase useCase = new WithdrawUseCase(txRepo, accRepo);

        WithdrawUseCaseTransactionalDecorator transactionalDecorator =
                new WithdrawUseCaseTransactionalDecorator(useCase, transactionTemplate);

        return new WithdrawUseCaseIdempotencyDecorator(transactionalDecorator, idempotencyTemplate);
    }

    @Bean
    public TransferMoneyPort transferMoneyPort(TransactionRepository txRepo,
                                               AccountRepository accRepo,
                                               TransactionTemplate transactionTemplate,
                                               IdempotencyTemplate idempotencyTemplate) {

        TransferMoneyUseCase useCase = new TransferMoneyUseCase(txRepo, accRepo);
        TransferMoneyUseCaseTransactionalDecorator transactionalDecorator =
                new TransferMoneyUseCaseTransactionalDecorator(useCase, transactionTemplate);

        return new TransferUseCaseIdempotencyDecorator(transactionalDecorator,idempotencyTemplate);
    }

    @Bean
    public GetTransactionsPort getTransactionsPort(TransactionRepository txRepo, AccountRepository accRepo) {
        GetTransactionsUseCase useCase = new GetTransactionsUseCase(txRepo, accRepo);
        return new GetTransactionsUseCaseTransactionalDecorator(useCase);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}