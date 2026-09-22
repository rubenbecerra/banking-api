package com.banking.transactions.domain.model;

public record PageQuery(int page, int size, String sortBy) {}