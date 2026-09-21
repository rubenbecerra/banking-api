package com.banking;

import com.banking.accounts.domain.model.Account;
import com.banking.accounts.application.usecase.CreateAccountPort;
import com.banking.accounts.infrastructure.persistence.SpringDataAccountRepository;
import com.banking.shared.security.JWTUtil;
import com.banking.accounts.domain.repository.AccountRepository;
import com.banking.transactions.domain.repository.TransactionRepository;
import com.banking.transactions.application.TransactionService;
import com.banking.transactions.domain.model.Transaction;
import com.banking.transactions.infrastructure.rest.DepositRequest;
import com.banking.transactions.infrastructure.rest.TransactionDTO;
import com.banking.transactions.infrastructure.rest.TransferRequest;
import com.banking.transactions.infrastructure.rest.WithdrawalRequest;
import com.banking.transactions.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionIntegrationTest extends AbstractTestContainers {
    @LocalServerPort
    private int port;

    @Autowired
    private SpringDataAccountRepository springDataAccountRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CreateAccountPort createAccountPort;

    @Autowired
    private TransactionService transactionService;

    private RestClient restClient;

    @Autowired
    private JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        springDataAccountRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("Must deposit valid amounts")
    void customerCanDepositValidAmount() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        DepositRequest depositRequest = new DepositRequest(account.getIban(), BigDecimal.valueOf(50));

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(depositRequest)
                .retrieve().toEntity(TransactionDTO.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().targetIban()).isEqualTo(account.getIban());
        assertThat(response.getBody().amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(response.getBody().type()).isEqualTo(TransactionType.DEPOSIT);

        Account updatedAccount = accountRepository.findByIban(account.getIban()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    @DisplayName("Must reject invalid deposit amounts (negative or zero)")
    void customerCannotDepositInvalidAmount() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        DepositRequest depositRequest = new DepositRequest(account.getIban(), BigDecimal.valueOf(-50));

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(depositRequest)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedAccount = accountRepository.findByIban(account.getIban())
                .orElseThrow(() -> new NoSuchElementException("Cannot found the account"));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("Must reject invalid withdrawal amounts (negative or zero)")
    void customerCannotWithdrawInvalidAmount() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.getIban(), BigDecimal.valueOf(-50));

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(withdrawalRequest)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedAccount = accountRepository.findByIban(account.getIban())
                .orElseThrow(() -> new NoSuchElementException("Cannot find the account"));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Must reject withdrawal when balance is insufficient")
    void customerCannotWithdrawMoreThanCurrentBalance() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.getIban(), BigDecimal.valueOf(150));

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(withdrawalRequest)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedAccount = accountRepository.findByIban(account.getIban())
                .orElseThrow(() -> new NoSuchElementException("Cannot find the account"));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("Must withdraw a valid amount")
    void customerCanWithdrawValidAmounts() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.getIban(), BigDecimal.valueOf(50));

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/withdraw")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(withdrawalRequest)
                .retrieve()
                .toEntity(TransactionDTO.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().sourceIban()).isEqualTo(account.getIban());
        assertThat(response.getBody().targetIban()).isNull();
        assertThat(response.getBody().type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getBody().timestamp()).isNotNull();

        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);

        Transaction savedTx = transactions.getFirst();
        assertThat(savedTx.getSourceIban()).isEqualTo(account.getIban());
        assertThat(savedTx.getTargetIban()).isNull();
        assertThat(savedTx.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(savedTx.getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    @DisplayName("Customer can transfer money to another account successfully")
    void customerCanTransferMoneySuccessfully() {
        String senderEmail = "sender@gmail.com";
        String receiverEmail = "receiver@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        Account sourceAccount = createAccountPort.execute(senderEmail, BigDecimal.valueOf(100));
        Account targetAccount = createAccountPort.execute(receiverEmail, BigDecimal.valueOf(50));

        TransferRequest transferRequest = new TransferRequest(
                sourceAccount.getIban(),
                targetAccount.getIban(),
                BigDecimal.valueOf(40)
        );

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/transfer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(transferRequest)
                .retrieve()
                .toEntity(TransactionDTO.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sourceIban()).isEqualTo(sourceAccount.getIban());
        assertThat(response.getBody().targetIban()).isEqualTo(targetAccount.getIban());
        assertThat(response.getBody().amount()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(response.getBody().type()).isEqualTo(TransactionType.TRANSFER);

        Account updatedSource = accountRepository.findByIban(sourceAccount.getIban()).orElseThrow();
        Account updatedTarget = accountRepository.findByIban(targetAccount.getIban()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(updatedTarget.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(90));

        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions.size()).isEqualTo(1);
        assertThat(transactions.getFirst().getType()).isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    @DisplayName("Must reject transfer when amount is negative or zero")
    void customerCannotTransferInvalidAmount() {
        String senderEmail = "sender@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        Account sourceAccount = createAccountPort.execute(senderEmail, BigDecimal.valueOf(100));
        Account targetAccount = createAccountPort.execute("receiver@gmail.com", BigDecimal.valueOf(50));

        TransferRequest transferRequest = new TransferRequest(
                sourceAccount.getIban(),
                targetAccount.getIban(),
                BigDecimal.valueOf(-10)
        );

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transferRequest)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedSource = accountRepository.findByIban(sourceAccount.getIban()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Must reject transfer when sender has insufficient balance")
    void customerCannotTransferWithInsufficientBalance() {
        String senderEmail = "sender@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        Account sourceAccount = createAccountPort.execute(senderEmail, BigDecimal.valueOf(30));
        Account targetAccount = createAccountPort.execute("receiver@gmail.com", BigDecimal.valueOf(50));

        TransferRequest transferRequest = new TransferRequest(
                sourceAccount.getIban(),
                targetAccount.getIban(),
                BigDecimal.valueOf(100)
        );

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transferRequest)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedSource = accountRepository.findByIban(sourceAccount.getIban()).orElseThrow();
        Account updatedTarget = accountRepository.findByIban(targetAccount.getIban()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(updatedTarget.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Must reject transfer to the same source account")
    void customerCannotTransferToSameAccount() {
        String senderEmail = "sender@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        Account account = createAccountPort.execute(senderEmail, BigDecimal.valueOf(100));

        TransferRequest transferRequest = new TransferRequest(
                account.getIban(),
                account.getIban(),
                BigDecimal.valueOf(20)
        );

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transferRequest)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Admin must deposit in any account")
    void adminCanDepositValidAmount() {
        String ownerEmail = "customer@gmail.com";
        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        String adminEmail = "admin@gmail.com";
        String token = jwtUtil.generateTestToken(adminEmail,List.of("ROLE_ADMIN"));

        DepositRequest depositRequest = new DepositRequest(account.getIban(), BigDecimal.valueOf(50));

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/admin/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(depositRequest)
                .retrieve().toEntity(TransactionDTO.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().targetIban()).isEqualTo(account.getIban());
        assertThat(response.getBody().amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(response.getBody().type()).isEqualTo(TransactionType.DEPOSIT);

        Account updatedAccount = accountRepository.findByIban(account.getIban()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    @DisplayName("Admin must withdraw a valid amount in any account")
    void adminCanWithdrawValidAmounts() {
        String ownerEmail = "customer@gmail.com";
        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        String adminEmail = "admin@gmail.com";
        String token = jwtUtil.generateTestToken(adminEmail, List.of("ROLE_ADMIN"));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.getIban(), BigDecimal.valueOf(50));

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/admin/withdraw")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(withdrawalRequest)
                .retrieve()
                .toEntity(TransactionDTO.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().sourceIban()).isEqualTo(account.getIban());
        assertThat(response.getBody().targetIban()).isNull();
        assertThat(response.getBody().type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getBody().timestamp()).isNotNull();

        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);

        Transaction savedTx = transactions.getFirst();
        assertThat(savedTx.getSourceIban()).isEqualTo(account.getIban());
        assertThat(savedTx.getTargetIban()).isNull();
        assertThat(savedTx.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(savedTx.getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    @DisplayName("Customer can retrieve transaction history for their own account")
    void customerCanRetrieveOwnTransactions() {
        String ownerEmail = "owner@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));

        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        DepositRequest depositRequest = new DepositRequest(account.getIban(), BigDecimal.valueOf(50));
        restClient.post()
                .uri("/api/v1/transactions/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(depositRequest)
                .retrieve()
                .toBodilessEntity();

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.getIban(), BigDecimal.valueOf(30));
        restClient.post()
                .uri("/api/v1/transactions/withdraw")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(withdrawalRequest)
                .retrieve()
                .toBodilessEntity();

        Map<String, Object> response = restClient.get()
                .uri("/api/v1/transactions/movements/{iban}", account.getIban())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response).isNotNull();
        assertThat(response.get("totalElements")).isEqualTo(2);

        List<?> content = (List<?>) response.get("content");
        assertThat(content).hasSize(2);
    }

    @Test
    @DisplayName("Customer cannot retrieve transactions of an account they do not own")
    void customerCannotViewTransactionsOfOtherAccounts() {
        String legitOwner = "legit@gmail.com";
        String attackerEmail = "attacker@gmail.com";
        String attackerToken = jwtUtil.generateTestToken(attackerEmail, List.of("ROLE_USER"));

        Account account = createAccountPort.execute(legitOwner, BigDecimal.valueOf(100));

        assertThatThrownBy(() ->
                restClient.get()
                        .uri("/api/v1/transactions/movements/{iban}", account.getIban())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + attackerToken)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.class).hasMessageContaining("403");
    }

    @Test
    @DisplayName("Admin can retrieve transactions for any account, normal user is forbidden")
    void adminCanRetrieveAnyAccountTransactions() {
        String clientEmail = "client@gmail.com";
        String adminEmail = "admin@bank.com";

        String clientToken = jwtUtil.generateTestToken(clientEmail, List.of("ROLE_USER"));
        String adminToken = jwtUtil.generateTestToken(adminEmail, List.of("ROLE_ADMIN"));

        Account account = createAccountPort.execute(clientEmail, BigDecimal.valueOf(200));

        DepositRequest depositRequest = new DepositRequest(account.getIban(), BigDecimal.valueOf(50));
        restClient.post()
                .uri("/api/v1/transactions/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(depositRequest)
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<Map<String, Object>> adminResponse = restClient.get()
                .uri("/api/v1/transactions/admin/movements/{iban}", account.getIban())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(adminResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(adminResponse.getBody()).isNotNull();

        Map<String, Object> body = adminResponse.getBody();
        assertThat(body.get("totalElements")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content).hasSize(1);
        assertThat(Double.valueOf(content.get(0).get("amount").toString())).isEqualTo(50.0);

        assertThatThrownBy(() ->
                restClient.get()
                        .uri("/api/v1/transactions/admin/movements/{iban}", account.getIban())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("403");
    }

    @Test
    @DisplayName("Concurrent withdrawals: only valid balance operations succeed without race conditions")
    void concurrentWithdrawalsSafetyTest() throws InterruptedException {
        String ownerEmail = "concurrent_withdraw@bank.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));

        Account account = createAccountPort.execute(ownerEmail, BigDecimal.valueOf(100));

        int totalThreads = 10;
        BigDecimal withdrawAmount = BigDecimal.valueOf(20);
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successfulTransactions = new AtomicInteger(0);
        AtomicInteger failedTransactions = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    WithdrawalRequest request = new WithdrawalRequest(account.getIban(), withdrawAmount);
                    restClient.post()
                            .uri("/api/v1/transactions/withdraw")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .toBodilessEntity();

                    successfulTransactions.incrementAndGet();
                } catch (Exception e) {
                    failedTransactions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successfulTransactions.get()).isEqualTo(5);
        assertThat(failedTransactions.get()).isEqualTo(5);

        Account updatedAccount = accountRepository.findByIban(account.getIban()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Concurrent bidirectional transfers: no deadlocks and total balance remains consistent")
    void concurrentBidirectionalTransfersSafetyTest() throws InterruptedException {
        String userA = "usera@bank.com";
        String userB = "userb@bank.com";

        String tokenA = jwtUtil.generateTestToken(userA, List.of("ROLE_USER"));
        String tokenB = jwtUtil.generateTestToken(userB, List.of("ROLE_USER"));

        Account accountA = createAccountPort.execute(userA, BigDecimal.valueOf(500));
        Account accountB = createAccountPort.execute(userB, BigDecimal.valueOf(500));

        int transfersPerDirection = 10;
        int totalThreads = transfersPerDirection * 2;
        BigDecimal amount = BigDecimal.valueOf(10);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);


        for (int i = 0; i < transfersPerDirection; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    TransferRequest request = new TransferRequest(accountA.getIban(), accountB.getIban(), amount);
                    restClient.post()
                            .uri("/api/v1/transactions/transfer")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        for (int i = 0; i < transfersPerDirection; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    TransferRequest request = new TransferRequest(accountB.getIban(), accountA.getIban(), amount);
                    restClient.post()
                            .uri("/api/v1/transactions/transfer")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Account finalA = accountRepository.findByIban(accountA.getIban()).orElseThrow();
        Account finalB = accountRepository.findByIban(accountB.getIban()).orElseThrow();

        BigDecimal totalFinalBalance = finalA.getBalance().add(finalB.getBalance());
        assertThat(totalFinalBalance).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Customer transactions are paginated and sorted correctly")
    void customerCanViewPaginatedTransactions() {
        String userEmail = "paginated_user@bank.com";
        String token = jwtUtil.generateTestToken(userEmail, List.of("ROLE_USER"));


        Account account = createAccountPort.execute(userEmail, BigDecimal.valueOf(1000));

        for (int i = 1; i <= 15; i++) {
            DepositRequest req = new DepositRequest(account.getIban(), BigDecimal.valueOf(10 + i));
            restClient.post()
                    .uri("/api/v1/transactions/deposit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
        }

        Map<String, Object> firstPage = restClient.get()
                .uri("/api/v1/transactions/movements/{iban}?page=0&size=5", account.getIban())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(firstPage).isNotNull();
        assertThat(firstPage.get("totalElements")).isEqualTo(15);
        assertThat(firstPage.get("totalPages")).isEqualTo(3);
        assertThat(firstPage.get("number")).isEqualTo(0);

        @SuppressWarnings("unchecked")
        List<?> firstPageContent = (List<?>) firstPage.get("content");
        assertThat(firstPageContent).hasSize(5);

        Map<String, Object> lastPage = restClient.get()
                .uri("/api/v1/transactions/movements/{iban}?page=2&size=5", account.getIban())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(lastPage).isNotNull();
        assertThat(lastPage.get("number")).isEqualTo(2);
        assertThat(lastPage.get("last")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<?> lastPageContent = (List<?>) lastPage.get("content");
        assertThat(lastPageContent).hasSize(5);
    }
}