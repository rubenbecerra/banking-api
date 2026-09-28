package com.banking.customers.application.usecase;

public interface DeleteCustomerPort {
    void deleteById(Integer id);
    void deleteByEmail(String email);
}
