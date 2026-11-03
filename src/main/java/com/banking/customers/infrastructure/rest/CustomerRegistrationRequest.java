package com.banking.customers.infrastructure.rest;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest (
        @NotBlank(message= "Name cannot be empty")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotNull(message = "Age cannot be empty")
        @Min(value = 18, message = "Customer must be at least 18")
        @Max(value = 120, message = "Customer must be at most 120")
        Integer age,

        @Size(max = 20, message = "Gender must be at most 20 characters")
        String gender,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password

){
}
