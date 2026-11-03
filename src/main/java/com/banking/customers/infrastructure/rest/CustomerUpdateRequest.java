package com.banking.customers.infrastructure.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Min(value = 18, message = "Customer must be at least 18")
        @Max(value = 120, message = "Customer must be at most 120")
        Integer age,

        @Size(max = 20, message = "Gender must be at most 20 characters")
        String gender
) {}
