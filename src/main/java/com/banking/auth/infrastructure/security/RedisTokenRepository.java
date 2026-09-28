package com.banking.auth.infrastructure.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisTokenRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "refresh_token:";

    public RedisTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String username, String refreshToken, Duration duration) {
        redisTemplate.opsForValue().set(PREFIX + username, refreshToken, duration);
    }

    public String get(String username) {
        return redisTemplate.opsForValue().get(PREFIX + username);
    }

    public void delete(String username) {
        redisTemplate.delete(PREFIX + username);
    }
}