package com.banking.transactions.infrastructure.rest;

import com.banking.transactions.application.usecase.DepositPort;
import com.banking.transactions.application.usecase.GetTransactionsPort;
import com.banking.transactions.application.usecase.TransferMoneyPort;
import com.banking.transactions.application.usecase.WithdrawPort;
import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final DepositPort depositPort;
    private final GetTransactionsPort getTransactionsPort;
    private final TransferMoneyPort transferMoneyPort;
    private final WithdrawPort withdrawPort;
    private final TransactionRestMapper restMapper;

    public TransactionController( TransactionRestMapper restMapper,
                                  DepositPort depositPort,
                                  TransferMoneyPort transferMoneyPort,
                                  WithdrawPort withdrawPort,
                                  GetTransactionsPort getTransactionsPort) {
        this.restMapper = restMapper;
        this.depositPort = depositPort;
        this.getTransactionsPort = getTransactionsPort;
        this.transferMoneyPort= transferMoneyPort;
        this.withdrawPort = withdrawPort;

    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> deposit(@Valid @RequestBody DepositRequest request,
                                                  Authentication authentication) {
        String ownerEmail = authentication.getName();
        Transaction transaction = depositPort.deposit(request.targetIban(),request.amount(),ownerEmail);

        TransactionDTO result = restMapper.toDTO(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @PostMapping("/admin/deposit")
    public ResponseEntity<TransactionDTO> adminDeposit(@Valid @RequestBody DepositRequest request) {
        Transaction transaction = depositPort.adminDeposit(request.targetIban(), request.amount());
        TransactionDTO result = restMapper.toDTO(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(@Valid @RequestBody WithdrawalRequest request,
                                                   Authentication authentication) {
        String ownerEmail = authentication.getName();
        Transaction transaction = withdrawPort.withdraw(ownerEmail,request.sourceIban(), request.amount());
        TransactionDTO result = restMapper.toDTO(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @PostMapping("/admin/withdraw")
    public ResponseEntity<TransactionDTO> adminWithdraw(@Valid @RequestBody WithdrawalRequest request) {
        Transaction transaction = withdrawPort.adminWithdraw(request.sourceIban(), request.amount());
        TransactionDTO result = restMapper.toDTO(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request,
                                                   Authentication authentication) {
        String ownerEmail = authentication.getName();
        Transaction transaction = transferMoneyPort.transferMoney(ownerEmail,
                request.sourceIban(), request.targetIban(), request.amount());
        TransactionDTO result = restMapper.toDTO(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/movements/{iban}")
    public ResponseEntity<PageResult<TransactionDTO>> viewTransactions(@PathVariable("iban") String iban,
                                                                       Authentication authentication,
                                                                       @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC)
                                                                 Pageable pageable) {
        String email = authentication.getName();

        PageResult<Transaction> domainResult = getTransactionsPort.getTransactionsForAccount(
                iban, email, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().iterator().next().getProperty()
        );

        return ResponseEntity.ok(restMapper.toPageDTO(domainResult));
    }

    @GetMapping("/admin/movements/{iban}")
    public ResponseEntity<PageResult<TransactionDTO>> getAccountTransactionsForAdmin(
            @PathVariable("iban") String iban,
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        String sortBy = pageable.getSort().isSorted()
                ? pageable.getSort().iterator().next().getProperty()
                : "timestamp";

        PageResult<Transaction> domainResult = getTransactionsPort.adminGetTransactionsByIban(iban, page, size, sortBy);


        return ResponseEntity.ok(restMapper.toPageDTO(domainResult));
    }
}

