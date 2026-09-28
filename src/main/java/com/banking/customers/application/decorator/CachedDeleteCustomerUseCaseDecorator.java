package com.banking.customers.application.decorator;

import com.banking.customers.application.usecase.DeleteCustomerPort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

public class CachedDeleteCustomerUseCaseDecorator implements DeleteCustomerPort {

    private final DeleteCustomerPort target;

    public CachedDeleteCustomerUseCaseDecorator(DeleteCustomerPort target) {
        this.target = target;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "customers", allEntries = true),
            @CacheEvict(value = "customer_by_email", allEntries = true)
    })
    public void deleteById(Integer id) {
        target.deleteById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "customers", allEntries = true),
            @CacheEvict(value = "customer_by_email", allEntries = true)
    })
    public void deleteByEmail(String email) {
        target.deleteByEmail(email);
    }
}
