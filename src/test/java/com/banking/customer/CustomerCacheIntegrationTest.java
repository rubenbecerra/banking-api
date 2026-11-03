package com.banking.customer;

import com.banking.AbstractTestContainers;
import com.banking.customers.application.usecase.GetCustomerPort;
import com.banking.customers.application.usecase.UpdateCustomerPort;
import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerCacheIntegrationTest extends AbstractTestContainers {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GetCustomerPort getCustomerPort;

    @Autowired
    private UpdateCustomerPort updateCustomerPort;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        clearCaches();
    }

    @Test
    @DisplayName("Customer reads are cached in Redis and evicted after an update")
    void shouldCacheCustomerReadsAndEvictOnUpdate() {
        String email = "cache-user@example.com";

        Customer saved = customerRepository.save(
                new Customer("Original", email, 30, "MALE", "encoded-password"));

        Customer firstRead = getCustomerPort.getCustomerByEmail(email);
        assertThat(firstRead.getName()).isEqualTo("Original");

        // Change the database behind the use case: the next read must still hit the cache.
        saved.setName("Changed directly in DB");
        customerRepository.save(saved);

        Customer cachedRead = getCustomerPort.getCustomerByEmail(email);
        assertThat(cachedRead.getName()).isEqualTo("Original");

        assertThat(getCustomerPort.getAllCustomers())
                .extracting(Customer::getEmail)
                .containsExactly(email);

        // The update use case must evict the affected entries.
        updateCustomerPort.updateByEmail(email, "Updated", 31, "MALE");

        Customer refreshedRead = getCustomerPort.getCustomerByEmail(email);
        assertThat(refreshedRead.getName()).isEqualTo("Updated");
        assertThat(refreshedRead.getAge()).isEqualTo(31);

        assertThat(getCustomerPort.getAllCustomers())
                .extracting(Customer::getName)
                .containsExactly("Updated");
    }

    private void clearCaches() {
        for (String name : List.of("customers", "customer_by_email", "customer_by_id")) {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
