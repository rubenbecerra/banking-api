package com.banking.transactions.infrastructure.rest;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest (
    @NotBlank(message = "Source IBAN is required")
    @Pattern(regexp = "^ES\\d{22}$", message = "Source IBAN must be a Spanish IBAN")
    String sourceIban,

    @NotBlank(message = "Target IBAN is required")
    @Pattern(regexp = "^ES\\d{22}$", message = "Target IBAN must be a Spanish IBAN")
    String targetIban,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer and 2 decimal digits")
    BigDecimal amount

)
{
}
