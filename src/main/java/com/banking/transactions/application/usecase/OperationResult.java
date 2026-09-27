package com.banking.transactions.application.usecase;

public record OperationResult<T>(T data, boolean isCacheHit) {

    public static <T> OperationResult<T> fresh(T data) {
        return new OperationResult<>(data, false);
    }

    public static <T> OperationResult<T> cached(T data) {
        return new OperationResult<>(data, true);
    }
}
