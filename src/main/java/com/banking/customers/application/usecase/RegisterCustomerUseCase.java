package com.banking.customers.application.usecase;

import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

public class RegisterCustomerUseCase implements RegisterCustomerPort{

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterCustomerUseCase(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void execute(String name, String email, Integer age, String gender, String rawPassword) {
        if (customerRepository.existsCustomerByEmail(email)) {
            throw new IllegalArgumentException("email already taken");
        }

        Customer customer = new Customer(
                name,
                email,
                age,
                gender,
                passwordEncoder.encode(rawPassword)
        );

        customerRepository.save(customer);
    }
}