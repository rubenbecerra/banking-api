package com.banking.auth.infrastructure.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(
        @NotBlank(message = "Username is required")
        @Email(message = "Username must be a valid email")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
