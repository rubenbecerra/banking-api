package com.banking.customers.application.decorator;

import com.banking.customers.application.usecase.UpdateCustomerPort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

public class CachedUpdateCustomerUseCaseDecorator implements UpdateCustomerPort {

    private final UpdateCustomerPort target;

    public CachedUpdateCustomerUseCaseDecorator(UpdateCustomerPort target) {
        this.target = target;
    }
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "customers", allEntries = true),
            @CacheEvict(value = "customer_by_email", allEntries = true)
    })
    public void update(Integer id, String name, Integer age, String gender) {
        target.update(id,name,age,gender);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "customers", allEntries = true),
            @CacheEvict(value = "customer_by_email", key = "#email")
    })
    public void updateByEmail(String email, String name, Integer age, String gender) {
        target.updateByEmail(email,name,age,gender);
    }
}
