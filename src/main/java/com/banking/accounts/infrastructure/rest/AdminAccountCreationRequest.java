package com.banking.accounts.infrastructure.rest;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AdminAccountCreationRequest(
        @NotBlank(message = "Owner email is required")
        @Email(message = "Owner email must be valid")
        String ownerEmail,

        @NotNull(message = "Initial balance is required")
        @PositiveOrZero(message = "Initial balance cannot be negative")
        @Digits(integer = 17, fraction = 2, message = "Initial balance must have at most 17 integer and 2 decimal digits")
        BigDecimal initialBalance
) {
}
