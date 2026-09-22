package com.banking.transactions.domain.model;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int number,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {}