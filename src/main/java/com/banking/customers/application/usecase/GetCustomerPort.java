package com.banking.customers.application.usecase;


import com.banking.customers.domain.model.Customer;

import java.util.List;


public interface GetCustomerPort {
    Customer getCustomerByEmail(String email);
    List<Customer> getAllCustomers();
    Customer getCustomerById(Integer id);
}
