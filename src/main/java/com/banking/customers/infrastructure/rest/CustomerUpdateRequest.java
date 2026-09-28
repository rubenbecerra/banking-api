package com.banking.customers.infrastructure.rest;

import jakarta.validation.constraints.Min;

public record CustomerUpdateRequest(
        String name,

        @Min(value = 18, message = "Customer must be at least 18")
        Integer age,
        String gender
) {}
