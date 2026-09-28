package com.banking.customers.infrastructure.rest;

import com.banking.customers.domain.model.Customer;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class CustomerDTOMapper implements Function<Customer, CustomerDTO> {
    @Override
    public CustomerDTO apply (Customer customer) {
        return new CustomerDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getAge(),
                customer.getGender()
        );
    }
}
