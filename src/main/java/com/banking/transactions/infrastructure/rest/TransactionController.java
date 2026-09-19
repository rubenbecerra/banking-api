package com.banking.transactions.infrastructure.rest;


import com.banking.transactions.application.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService =transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> deposit(@Valid @RequestBody DepositRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.deposit(request,ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @PostMapping("/admin/deposit")
    public ResponseEntity<TransactionDTO> adminDeposit(@Valid @RequestBody DepositRequest request) {
        TransactionDTO result = transactionService.adminDeposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(@Valid @RequestBody WithdrawalRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.withdraw(ownerEmail,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @PostMapping("/admin/withdraw")
    public ResponseEntity<TransactionDTO> adminWithdraw(@Valid @RequestBody WithdrawalRequest request) {
        TransactionDTO result = transactionService.adminWithdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.transferMoney(ownerEmail,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/movements/{iban}")
    public ResponseEntity<Page<TransactionDTO>> viewTransactions(@PathVariable("iban") String iban,
                                                                 Authentication authentication,
                                                                 @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC)
                                                                 Pageable pageable) {
        String email = authentication.getName();
        return ResponseEntity.status(HttpStatus.OK).body(transactionService.getTransactionsForAccount(iban,email,pageable));
    }
    @GetMapping("/admin/movements/{iban}")
    public ResponseEntity<Page<TransactionDTO>> getAccountTransactionsForAdmin(@PathVariable("iban") String iban,
                                                                               @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC)
                                                                               Pageable pageable) {
        return ResponseEntity.ok(transactionService.adminGetTransactionsByIban(iban,pageable));
    }
}

