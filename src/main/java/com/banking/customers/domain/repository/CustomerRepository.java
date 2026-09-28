package com.banking.customers.domain.repository;

import com.banking.customers.domain.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    List<Customer> findAll();
    Optional<Customer> findById(Integer id);
    Optional<Customer> findByEmail(String email);
    boolean existsById(Integer id);
    boolean existsCustomerByEmail(String email);
    Customer save(Customer customer);
    void deleteById(Integer id);
    void delete(Customer customer);
    void deleteAll();
}
