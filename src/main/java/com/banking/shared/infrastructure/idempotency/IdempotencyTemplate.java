package com.banking.shared.infrastructure.idempotency;

import com.banking.shared.infrastructure.redis.IdempotencyRepository;
import com.banking.shared.security.IdempotencyContextHolder;
import com.banking.transactions.application.usecase.OperationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.function.Supplier;

public class IdempotencyTemplate {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    private static final String PROCESSING_STATUS = "PROCESSING_LOCK";

    public IdempotencyTemplate(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    public <T> OperationResult<T> execute(String actionPrefix, Class<T> responseType, Supplier<OperationResult<T>> action) {
        String idempotencyKey = IdempotencyContextHolder.getCurrentIdempotencyKey();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String redisKey = actionPrefix + ":" + idempotencyKey;

        boolean acquired = idempotencyRepository.setIfAbsent(redisKey, PROCESSING_STATUS, Duration.ofMinutes(5));

        if (!acquired) {
            String cachedValue = idempotencyRepository.get(redisKey);
            if (PROCESSING_STATUS.equals(cachedValue)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction in course. Retry in a few seconds.");
            }
            try {
                T cachedData = objectMapper.readValue(cachedValue, responseType);
                return OperationResult.cached(cachedData);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing the response", e);
            }
        }

        try {
            OperationResult<T> result = action.get();

            try {
                String jsonResult = objectMapper.writeValueAsString(result.data());
                idempotencyRepository.save(redisKey, jsonResult, Duration.ofHours(24));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing the transaction", e);
            }

            return result;
        } catch (Exception e) {
            idempotencyRepository.delete(redisKey);
            throw e;
        }
    }
}