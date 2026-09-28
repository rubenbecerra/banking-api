package com.banking.customers.infrastructure.persistence;

import com.banking.customers.domain.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerEntityMapper {

    public Customer toDomain(CustomerJpaEntity entity) {
        if (entity == null) return null;
        return new Customer(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getAge(),
                entity.getGender(),
                entity.getPassword(),
                entity.getRole()
        );
    }

    public CustomerJpaEntity toEntity(Customer domain) {
        if (domain == null) return null;
        return new CustomerJpaEntity(
                domain.getId(),
                domain.getName(),
                domain.getEmail(),
                domain.getAge(),
                domain.getGender(),
                domain.getPassword(),
                domain.getRole()
        );
    }
}