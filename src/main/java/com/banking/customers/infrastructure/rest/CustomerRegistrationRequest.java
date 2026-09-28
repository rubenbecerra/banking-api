package com.banking.customers.infrastructure.rest;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRegistrationRequest (
        @NotBlank(message= "Name cannot be empty")
        String name,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Age cannot be empty")
        @Min(value = 18, message = "Customer must be at least 18")
        Integer age,

        String gender,

        @NotBlank
        String password

){
}
