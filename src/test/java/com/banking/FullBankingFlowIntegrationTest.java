package com.banking;

import com.banking.accounts.infrastructure.rest.AccountDTO;
import com.banking.auth.infrastructure.rest.AuthenticationRequest;
import com.banking.customers.infrastructure.rest.CustomerRegistrationRequest;
import com.banking.transactions.infrastructure.rest.DepositRequest;
import com.banking.transactions.infrastructure.rest.TransactionDTO;
import com.banking.transactions.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FullBankingFlowIntegrationTest extends AbstractTestContainers {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @DisplayName("E2E Flow: Register, Login, Create Account, and Deposit Money")
    void shouldExecuteCompleteBankingFlow() {
        String email = "flowuser@gmail.com";
        String password = "SecurePassword123";

        CustomerRegistrationRequest registerRequest = new CustomerRegistrationRequest(
                "Flow User",
                email,
                28,
                "MALE",
                password
        );

        ResponseEntity<Void> registerResponse = restClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .retrieve()
                .toBodilessEntity();

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        AuthenticationRequest authReq = new AuthenticationRequest(email, password);

        ResponseEntity<Void> loginResponse = restClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(authReq)
                .retrieve()
                .toBodilessEntity();

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotNull();
        String sessionCookie = setCookieHeader.split(";")[0];

        ResponseEntity<AccountDTO> accountResponse = restClient.post()
                .uri("/api/v1/accounts/me")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HttpEntity<>(new HttpHeaders()))
                .retrieve()
                .toEntity(AccountDTO.class);

        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(accountResponse.getBody()).isNotNull();

        String userIban = accountResponse.getBody().iban();
        assertThat(userIban).startsWith("ES");
        assertThat(accountResponse.getBody().balance()).isEqualByComparingTo(BigDecimal.ZERO);

        BigDecimal depositAmount = BigDecimal.valueOf(250.00);
        DepositRequest depositRequest = new DepositRequest(userIban, depositAmount);

        ResponseEntity<TransactionDTO> depositResponse = restClient.post()
                .uri("/api/v1/transactions/deposit")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .body(depositRequest)
                .retrieve()
                .toEntity(TransactionDTO.class);

        assertThat(depositResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(depositResponse.getBody()).isNotNull();
        assertThat(depositResponse.getBody().targetIban()).isEqualTo(userIban);
        assertThat(depositResponse.getBody().amount()).isEqualByComparingTo(depositAmount);
        assertThat(depositResponse.getBody().type()).isEqualTo(TransactionType.DEPOSIT);
    }
}