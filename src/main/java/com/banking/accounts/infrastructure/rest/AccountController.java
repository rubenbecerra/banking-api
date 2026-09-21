package com.banking.accounts.infrastructure.rest;

import com.banking.accounts.application.usecase.CreateAccountPort;
import com.banking.accounts.application.usecase.DeleteAccountPort;
import com.banking.accounts.application.usecase.GetAccountsPort;
import com.banking.accounts.domain.model.Account;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final GetAccountsPort getAccountsPort;
    private final DeleteAccountPort deleteAccountPort;
    private final CreateAccountPort createAccountPort;

    public AccountController (GetAccountsPort getAccountsPort, DeleteAccountPort deleteAccountPort,
                             CreateAccountPort createAccountPort) {
        this.createAccountPort = createAccountPort;
        this.deleteAccountPort = deleteAccountPort;
        this.getAccountsPort = getAccountsPort;

    }

    @PostMapping("/me")
    public ResponseEntity<AccountDTO> createAccount(Authentication authentication) {
        String ownerEmail = authentication.getName();
        BigDecimal balance = BigDecimal.ZERO;
        Account createdAccount = createAccountPort.execute(ownerEmail, balance);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(createdAccount));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountDTO> createAccountForCustomer(@RequestBody AdminAccountCreationRequest request) {
        String ownerEmail = request.ownerEmail();
        BigDecimal balance = request.initialBalance();
        Account createdAccount = createAccountPort.execute(ownerEmail,balance);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(createdAccount));

    }

    @GetMapping("/me")
    public ResponseEntity<List<AccountDTO>> getUserAccounts(Authentication authentication) {
        String ownerEmail = authentication.getName();
        List<AccountDTO> response = getAccountsPort.getByOwner(ownerEmail).stream()
                .map(this::mapToDTO).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountDTO>> getAllUsersAccounts() {
        List<AccountDTO> response = getAccountsPort.getAll().stream()
                .map(this::mapToDTO).toList();
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("{iban}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(Authentication authentication, @PathVariable("iban") String iban) {
        String owner = authentication.getName();
        deleteAccountPort.execute(owner, iban);
    }

    @DeleteMapping("/admin/{iban}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccountByIban(@PathVariable("iban") String iban) {
        deleteAccountPort.executeByAdmin(iban);
    }
    private AccountDTO mapToDTO(Account account) {
        return new AccountDTO(
                account.getIban(),
                account.getBalance(),
                account.getOwner()
        );
    }
}
