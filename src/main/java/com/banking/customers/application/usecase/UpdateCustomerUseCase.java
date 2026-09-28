package com.banking.customers.application.usecase;


import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.repository.CustomerRepository;

import java.util.NoSuchElementException;

public class UpdateCustomerUseCase implements  UpdateCustomerPort{

    private final CustomerRepository customerRepository;

    public UpdateCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void update(Integer id, String name, Integer age, String gender) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer with ID " + id + " not found"));

        applyChanges(customer, name, age, gender);
        customerRepository.save(customer);
    }


    @Override
    public void updateByEmail(String email, String name, Integer age, String gender) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Client with email " + email + " not found"));

        applyChanges(customer, name, age, gender);
        customerRepository.save(customer);
    }

    private void applyChanges(Customer customer, String name, Integer age, String gender) {
        boolean changes = false;
        if (name != null && !name.isEmpty() && !name.equals(customer.getName())) {
            customer.setName(name);
            changes = true;
        }
        if (age != null && !age.equals(customer.getAge())) {
            customer.setAge(age);
            changes = true;
        }
        if (gender != null && !gender.isEmpty() && !gender.equals(customer.getGender())) {
            customer.setGender(gender);
            changes = true;
        }
        if (!changes) {
            throw new IllegalArgumentException("No data changes found");
        }
    }
}