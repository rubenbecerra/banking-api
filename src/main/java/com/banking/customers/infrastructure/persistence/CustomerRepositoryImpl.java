package com.banking.customers.infrastructure.persistence;

import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.repository.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepositoryImpl implements CustomerRepository {

    private final SpringDataCustomerRepository springDataRepository;
    private final CustomerEntityMapper mapper;

    public CustomerRepositoryImpl(SpringDataCustomerRepository springDataRepository, CustomerEntityMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Customer> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return springDataRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public boolean existsCustomerByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = mapper.toEntity(customer);
        CustomerJpaEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public void delete(Customer customer) {
        springDataRepository.delete(mapper.toEntity(customer));
    }
    @Override
    public void deleteAll() {
        springDataRepository.deleteAll();
    }
}