package com.banking.shared.security;

public class IdempotencyContextHolder {

    private static final ThreadLocal<String> idempotencyKeyHolder = new ThreadLocal<>();

    public static void setCurrentIdempotencyKey(String key) {
        idempotencyKeyHolder.set(key);
    }

    public static String getCurrentIdempotencyKey() {
        return idempotencyKeyHolder.get();
    }

    public static void clear() {
        idempotencyKeyHolder.remove();
    }
}