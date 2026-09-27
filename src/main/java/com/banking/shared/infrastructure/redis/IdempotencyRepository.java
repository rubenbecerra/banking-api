package com.banking.shared.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class IdempotencyRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "idempotency:";

    public IdempotencyRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String idempotencyKey, String value, Duration duration) {
        redisTemplate.opsForValue().set(PREFIX + idempotencyKey, value, duration);
    }

    public String get(String idempotencyKey) {
        return redisTemplate.opsForValue().get(PREFIX + idempotencyKey);
    }

    public boolean setIfAbsent(String idempotencyKey, String value, Duration duration) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(PREFIX + idempotencyKey, value, duration);
        return Boolean.TRUE.equals(success);
    }

    public void delete(String idempotencyKey) {
        redisTemplate.delete(PREFIX + idempotencyKey);
    }
}