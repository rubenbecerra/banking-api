package com.banking.customers.application.usecase;

import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.repository.CustomerRepository;

import java.util.NoSuchElementException;


public class DeleteCustomerUseCase implements  DeleteCustomerPort {

    private final CustomerRepository customerRepository;

    public DeleteCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }


    @Override
    public void deleteById(Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new NoSuchElementException("Client with ID " + id + " doesn't exist");
        }
        customerRepository.deleteById(id);
    }


    @Override
    public void deleteByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Client with email " + email + " doesn't exist"));
        customerRepository.delete(customer);
    }
}