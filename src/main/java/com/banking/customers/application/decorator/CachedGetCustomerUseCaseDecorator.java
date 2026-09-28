package com.banking.customers.application.decorator;

import com.banking.customers.application.usecase.GetCustomerPort;
import com.banking.customers.domain.model.Customer;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public class CachedGetCustomerUseCaseDecorator implements GetCustomerPort {

    private final GetCustomerPort target;

    public CachedGetCustomerUseCaseDecorator(GetCustomerPort target) {
        this.target = target;
    }
    @Override
    public List<Customer> getAllCustomers() {
        return target.getAllCustomers();
    }

    @Override
    @Cacheable(value = "customer_by_email", key = "#email")

    public Customer getCustomerByEmail(String email) {
        return target.getCustomerByEmail(email);
    }

    @Override
    public Customer getCustomerById(Integer id) {
        return target.getCustomerById(id);
    }
}
