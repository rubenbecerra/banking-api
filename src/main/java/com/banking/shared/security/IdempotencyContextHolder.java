package com.banking.shared.security;

public class IdempotencyContextHolder {

    private static final ThreadLocal<String> idempotencyKeyHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> authenticatedUserHolder = new ThreadLocal<>();

    public static void setCurrentIdempotencyKey(String key, String authenticatedUser) {
        idempotencyKeyHolder.set(key);
        authenticatedUserHolder.set(authenticatedUser);
    }

    public static String getCurrentIdempotencyKey() {
        return idempotencyKeyHolder.get();
    }

    public static String getCurrentUser() {
        return authenticatedUserHolder.get();
    }

    public static void clear() {
        idempotencyKeyHolder.remove();
        authenticatedUserHolder.remove();
    }
}
