package com.banking.transactions.domain.model;

import java.util.Set;

public record PageQuery(int page, int size, String sortBy) {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "timestamp", "amount", "type", "sourceIban", "targetIban");

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }
    }
}
