package com.banking.customers.application.usecase;

public interface RegisterCustomerPort {
    void execute(String name, String email, Integer age, String gender, String rawPassword);
}
