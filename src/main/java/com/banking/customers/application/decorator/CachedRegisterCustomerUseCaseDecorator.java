package com.banking.customers.application.decorator;

import com.banking.customers.application.usecase.RegisterCustomerPort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

public class CachedRegisterCustomerUseCaseDecorator implements RegisterCustomerPort {

    private final RegisterCustomerPort target;

    public CachedRegisterCustomerUseCaseDecorator(RegisterCustomerPort target) {
        this.target = target;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "customers", allEntries = true),
            @CacheEvict(value = "customer_by_email", allEntries = true)
    })
    public void execute(String name, String email, Integer age, String gender, String rawPassword) {
        target.execute(name,email,age,gender,rawPassword);
    }
}
