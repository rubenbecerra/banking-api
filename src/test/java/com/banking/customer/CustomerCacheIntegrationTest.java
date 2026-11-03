package com.banking.customer;

import com.banking.Main;
import com.banking.customers.application.usecase.GetCustomerPort;
import com.banking.customers.application.usecase.UpdateCustomerPort;
import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Dedicated context and Redis instance: cache state and the other integration tests
 * cannot interfere with each other, which keeps this test deterministic in CI.
 */
@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "test.context=customer-cache")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerCacheIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @ServiceConnection
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-only-jwt-secret-with-at-least-32-characters");
    }

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
        String email = "cache-" + UUID.randomUUID() + "@example.com";

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
                .contains(email);

        // The update use case must evict the affected entries. Redis eviction is
        // asynchronous, so wait briefly for the entry to disappear.
        updateCustomerPort.updateByEmail(email, "Updated", 31, "MALE");
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(cacheManager.getCache("customer_by_email").get(email)).isNull());

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
