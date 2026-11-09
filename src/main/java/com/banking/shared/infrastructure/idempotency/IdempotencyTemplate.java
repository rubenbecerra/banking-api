package com.banking.shared.infrastructure.idempotency;

import com.banking.shared.idempotency.domain.exception.DuplicateIdempotencyKeyException;
import com.banking.shared.idempotency.domain.model.IdempotencyKey;
import com.banking.shared.idempotency.domain.model.IdempotencyStatus;
import com.banking.shared.idempotency.domain.repository.IdempotencyKeyRepository;
import com.banking.shared.security.IdempotencyContextHolder;
import com.banking.transactions.application.usecase.OperationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * Executes a money-moving operation at most once per (principal, scope, key).
 *
 * The idempotency row, the business writes and the stored response are committed
 * in the same database transaction. If the business fails, the row disappears
 * with it, so a retry can execute again.
 *
 * On a duplicate key the transaction has already been rolled back by Postgres;
 * the stored response is read afterwards, in a fresh transaction.
 */
public class IdempotencyTemplate {

    private static final long RETENTION_HOURS = 24;

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public IdempotencyTemplate(IdempotencyKeyRepository repository,
                               ObjectMapper objectMapper,
                               TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public <T> OperationResult<T> execute(String scope,
                                          String fingerprint,
                                          Class<T> responseType,
                                          Supplier<OperationResult<T>> action) {

        String idempotencyKey = IdempotencyContextHolder.getCurrentIdempotencyKey();

        // No header -> no idempotency contract. Still run inside a transaction.
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return transactionTemplate.execute(status -> action.get());
        }

        String user = IdempotencyContextHolder.getCurrentUser();
        String principal = (user == null || user.isBlank()) ? "anonymous" : user;
        String requestFingerprint = sha256(fingerprint);
        LocalDateTime now = LocalDateTime.now();

        try {
            return transactionTemplate.execute(status -> {
                IdempotencyKey inserted = repository.insert(new IdempotencyKey(
                        null,
                        principal,
                        scope,
                        idempotencyKey,
                        IdempotencyStatus.PROCESSING,
                        null,
                        requestFingerprint,
                        now,
                        null,
                        now.plusHours(RETENTION_HOURS)
                ));

                OperationResult<T> result = action.get();

                repository.complete(inserted.id(), toJson(result.data()), LocalDateTime.now());

                return result;
            });
        } catch (DuplicateIdempotencyKeyException e) {
            // The transaction is already rolled back here. Read in a new one.
            return replay(principal, scope, idempotencyKey, requestFingerprint, responseType);
        }
    }

    private <T> OperationResult<T> replay(String principal,
                                          String scope,
                                          String idempotencyKey,
                                          String requestFingerprint,
                                          Class<T> responseType) {

        IdempotencyKey existing = repository.find(principal, scope, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Duplicate idempotency key but no stored row: " + idempotencyKey));

        // Same key, different payload: never return the old response silently.
        if (!existing.requestFingerprint().equals(requestFingerprint)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Idempotency key reused with a different request payload");
        }

        // Defensive: committed rows are COMPLETED, but a manually inserted row
        // (or a future two-phase implementation) could be in flight.
        if (existing.status() == IdempotencyStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Operation in progress. Retry in a few seconds.");
        }

        try {
            T data = objectMapper.readValue(existing.responseBody(), responseType);
            return OperationResult.cached(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot deserialize stored idempotent response", e);
        }
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize idempotent response", e);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
