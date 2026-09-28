package com.banking.accounts;

import com.banking.accounts.application.usecase.CreateAccountUseCase;
import com.banking.accounts.application.usecase.DeleteAccountUseCase;
import com.banking.accounts.application.usecase.GetAccountsUseCase;
import com.banking.accounts.domain.model.Account;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.shared.exceptions.exception.UnauthorizedActionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountUseCaseTest {

    @Mock
    private AccountRepository repo;

    @InjectMocks
    private CreateAccountUseCase createAccountUseCase;
    @InjectMocks
    private GetAccountsUseCase getAccountsUseCase;
    @InjectMocks
    private DeleteAccountUseCase deleteAccountUseCase;

    @Test
    @DisplayName("Should retrieve the accounts of an user")
    void getAccountsByOwnerTest() {
        String ownerEmail = "test1@gmail.com";

        List<Account> accounts = List.of(
                new Account("ES123456789", BigDecimal.valueOf(100.0), ownerEmail),
                new Account("ES987654321", BigDecimal.valueOf(500.0), ownerEmail)
        );

        when(repo.findAllByOwner(ownerEmail)).thenReturn(accounts);

        List<Account> result = getAccountsUseCase.getByOwner(ownerEmail);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getOwner()).isEqualTo(ownerEmail);

        verify(repo, times(1)).findAllByOwner(ownerEmail);
    }

    @Test
    @DisplayName("Should create an account generating an IBAN and saving to the repo")
    void shouldCreateAccount() {
        String ownerEmail = "test@gmail.com";
        BigDecimal balance =BigDecimal.valueOf(200);

        when(repo.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = createAccountUseCase.execute(ownerEmail, balance);

        assertThat(result).isNotNull();
        assertThat(result.getOwner()).isEqualTo(ownerEmail);
        assertThat(result.getBalance()).isEqualByComparingTo(balance);
        assertThat(result.getIban()).isNotNull();
        assertThat(result.getIban()).startsWith("ES");
        verify(repo, times(1)).save(any(Account.class));

    }

    @Test
    @DisplayName("Should delete the owner account")
    void shouldDeleteAccountSuccessfully() {
        // Arrange
        String iban = "ES123456789";
        String owner = "user@gmail.com";
        Account mockAccount = new Account(iban, BigDecimal.valueOf(100), owner);

        when(repo.findByIban(iban)).thenReturn(Optional.of(mockAccount));
        doNothing().when(repo).delete(mockAccount);

        deleteAccountUseCase.execute(owner, iban);

        verify(repo, times(1)).findByIban(iban);
        verify(repo, times(1)).delete(mockAccount);
    }

    @Test
    @DisplayName("Should throw an exception if the user is not the account owner")
    void shouldThrowExceptionWhenUnauthorizedUserDeletes() {
        String iban = "ES123456789";
        String realOwner = "owner@gmail.com";
        String attacker = "attacker@gmail.com";
        Account mockAccount = new Account(iban, BigDecimal.valueOf(100), realOwner);

        when(repo.findByIban(iban)).thenReturn(Optional.of(mockAccount));

        assertThatThrownBy(() -> deleteAccountUseCase.execute(attacker, iban))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    @DisplayName("Admin should delete any account")
    void shouldDeleteAccountByAdmin() {
        String iban = "ES123456789";
        Account mockAccount = new Account(iban, BigDecimal.valueOf(100), "user@gmail.com");

        when(repo.findByIban(iban)).thenReturn(Optional.of(mockAccount));
        doNothing().when(repo).delete(mockAccount);

        deleteAccountUseCase.executeByAdmin(iban);

        verify(repo, times(1)).findByIban(iban);
        verify(repo, times(1)).delete(mockAccount);
    }
}