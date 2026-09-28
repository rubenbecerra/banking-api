package com.banking.customers.application.usecase;

public interface UpdateCustomerPort {
    void update(Integer id, String name, Integer age, String gender);
    void updateByEmail(String email, String name, Integer age, String gender);
}
