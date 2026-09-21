package com.banking.transactions.application;

import com.banking.accounts.domain.exception.AccountNotFoundException;
import com.banking.transactions.infrastructure.rest.DepositRequest;
import com.banking.transactions.infrastructure.rest.TransactionDTO;
import com.banking.transactions.infrastructure.rest.TransferRequest;
import com.banking.transactions.infrastructure.rest.WithdrawalRequest;
import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.transactions.domain.repository.TransactionRepository;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this. accountRepository = accountRepository;
    }


    public TransactionDTO transferMoney(String ownerEmail, TransferRequest request) {
        if (request.sourceIban().equals(request.targetIban())) {
            throw new IllegalArgumentException("Source and target IBAN cannot be the same");
        }

        Account source;
        Account target;

        if (request.sourceIban().compareTo(request.targetIban()) < 0) {
            source = accountRepository.findByIbanWithLock(request.sourceIban())
                    .orElseThrow(() -> new NoSuchElementException("Source account not found"));
            target = accountRepository.findByIbanWithLock(request.targetIban())
                    .orElseThrow(() -> new NoSuchElementException("Target account not found"));
        } else {
            target = accountRepository.findByIbanWithLock(request.targetIban())
                    .orElseThrow(() -> new NoSuchElementException("Target account not found"));
            source = accountRepository.findByIbanWithLock(request.sourceIban())
                    .orElseThrow(() -> new NoSuchElementException("Source account not found"));
        }

        if (!source.getOwner().equals(ownerEmail)) {
            throw new AccessDeniedException("You are not the owner of the source account");
        }

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the transfer");
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        target.setBalance(target.getBalance().add(request.amount()));

        Transaction transaction = new Transaction(
                source.getIban(),
                target.getIban(),
                request.amount(),
                TransactionType.TRANSFER
        );
        accountRepository.save(target);
        accountRepository.save(source);
        Transaction saved = transactionRepository.save(transaction);
        return mapToDTO(saved);
    }
    public TransactionDTO deposit(DepositRequest request, String ownerEmail) {
        Account target = accountRepository.findByIbanWithLock(request.targetIban())
                .orElseThrow(() -> new AccountNotFoundException("Target account cannot be found"));
        if (!target.getOwner().equals(ownerEmail)) {
            throw new AccessDeniedException("You can only deposit money into your own accounts");
        }

        target.setBalance(target.getBalance().add(request.amount()));
        Transaction transaction = Transaction.createDeposit(
                target.getIban(),
                request.amount()
        );
        accountRepository.save(target);
        Transaction saved = transactionRepository.save(transaction);
        return mapToDTO(saved);
    }
    public TransactionDTO adminDeposit(DepositRequest request) {
        Account target = accountRepository.findByIbanWithLock(request.targetIban())
                .orElseThrow(() -> new AccountNotFoundException("Target account cannot be found"));
        target.setBalance(target.getBalance().add(request.amount()));
        Transaction transaction = Transaction.createDeposit(
                target.getIban(),
                request.amount()
        );
        accountRepository.save(target);
        Transaction saved = transactionRepository.save(transaction);
        return mapToDTO(saved);
    }

    public TransactionDTO withdraw(String ownerEmail, WithdrawalRequest request) {
        Account source = accountRepository.findByIbanWithLock(request.sourceIban())
                .orElseThrow(() -> new NoSuchElementException("Source account cannot be found"));
        if (!source.getOwner().equals(ownerEmail)) {
            throw new AccessDeniedException("You are not the owner of the source account");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the operation");
        }
        source.setBalance(source.getBalance().subtract(request.amount()));

        Transaction transaction = Transaction.createWithdrawal(
                source.getIban(),
                request.amount()
        );
        accountRepository.save(source);
        Transaction saved = transactionRepository.save(transaction);

        return mapToDTO(saved);
    }
    public TransactionDTO adminWithdraw(WithdrawalRequest request) {
        Account source = accountRepository.findByIbanWithLock(request.sourceIban())
                .orElseThrow(() -> new NoSuchElementException("Source account cannot be found"));

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the operation");
        }
        source.setBalance(source.getBalance().subtract(request.amount()));

        Transaction transaction = Transaction.createWithdrawal(
                source.getIban(),
                request.amount()
        );
        accountRepository.save(source);
        Transaction saved = transactionRepository.save(transaction);

        return mapToDTO(saved);
    }
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsForAccount(String iban, String authenticatedEmail,
                                                          Pageable pageable) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementException("Account cannot be found"));
        if (!account.getOwner().equals(authenticatedEmail)) {
            throw new AccessDeniedException("You are not authorized to view transactions for this account");
        }
        return transactionRepository.findBySourceIbanOrTargetIbanOrderByTimestampDesc(iban,iban, pageable)
                .map(this::mapToDTO);

    }
    @Transactional(readOnly = true)
    public Page<TransactionDTO> adminGetTransactionsByIban(String iban, Pageable pageable) {
        if (!accountRepository.existsByIban(iban)) {
            throw new NoSuchElementException("Account cannot be found with IBAN: " + iban);
        }

        return transactionRepository.findBySourceIbanOrTargetIbanOrderByTimestampDesc(iban, iban, pageable)
                .map(this::mapToDTO);
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getSourceIban(),
                transaction.getTargetIban(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getTimestamp()
        );
    }
}
