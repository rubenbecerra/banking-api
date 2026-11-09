package com.banking.transactions.infrastructure.config;

import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.ledger.domain.repository.LedgerEntryRepository;
import com.banking.ledger.domain.repository.MovementRepository;
import com.banking.shared.idempotency.domain.repository.IdempotencyKeyRepository;
import com.banking.shared.infrastructure.idempotency.IdempotencyTemplate;
import com.banking.transactions.application.decorator.idempotency.DepositUseCaseIdempotencyDecorator;
import com.banking.transactions.application.decorator.idempotency.TransferUseCaseIdempotencyDecorator;
import com.banking.transactions.application.decorator.idempotency.WithdrawUseCaseIdempotencyDecorator;
import com.banking.transactions.application.decorator.transactional.GetTransactionsUseCaseTransactionalDecorator;
import com.banking.transactions.application.usecase.*;
import com.banking.transactions.domain.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionUseCaseConfig {

    @Bean
    public IdempotencyTemplate idempotencyTemplate(IdempotencyKeyRepository idempotencyKeyRepository,
                                                   ObjectMapper objectMapper,
                                                   TransactionTemplate transactionTemplate) {
        return new IdempotencyTemplate(idempotencyKeyRepository, objectMapper, transactionTemplate);
    }

    @Bean
    public DepositPort depositPort(TransactionRepository txRepo,
                                   AccountRepository accRepo,
                                   MovementRepository movementRepository,
                                   LedgerEntryRepository ledgerEntryRepository,
                                   IdempotencyTemplate idempotencyTemplate) {

        DepositUseCase useCase = new DepositUseCase(txRepo, accRepo,
                movementRepository, ledgerEntryRepository);

        return new DepositUseCaseIdempotencyDecorator(useCase, idempotencyTemplate);
    }

    @Bean
    public WithdrawPort withdrawPort(TransactionRepository txRepo,
                                     AccountRepository accRepo,
                                     MovementRepository movementRepository,
                                     LedgerEntryRepository ledgerEntryRepository,
                                     IdempotencyTemplate idempotencyTemplate) {

        WithdrawUseCase useCase = new WithdrawUseCase(txRepo, accRepo,
                movementRepository, ledgerEntryRepository);

        return new WithdrawUseCaseIdempotencyDecorator(useCase, idempotencyTemplate);
    }

    @Bean
    public TransferMoneyPort transferMoneyPort(TransactionRepository txRepo,
                                               AccountRepository accRepo,
                                               MovementRepository movementRepository,
                                               LedgerEntryRepository ledgerEntryRepository,
                                               IdempotencyTemplate idempotencyTemplate) {

        TransferMoneyUseCase useCase = new TransferMoneyUseCase(txRepo, accRepo,
                movementRepository, ledgerEntryRepository);

        return new TransferUseCaseIdempotencyDecorator(useCase, idempotencyTemplate);
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
