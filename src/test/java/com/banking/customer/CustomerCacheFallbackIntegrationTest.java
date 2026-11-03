package com.banking.customer;

import com.banking.Main;
import com.banking.customers.application.usecase.GetCustomerPort;
import com.banking.customers.application.usecase.RegisterCustomerPort;
import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dedicated context so stopping Redis cannot affect the other integration tests.
 */
@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.data.redis.timeout=250ms",
                "spring.data.redis.connect-timeout=250ms",
                "test.context=redis-fallback"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerCacheFallbackIntegrationTest {

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
    private RegisterCustomerPort registerCustomerPort;

    @Test
    void shouldFallBackToDatabaseAndKeepWritesWorkingWhenRedisIsDown() {
        String email = "fallback@example.com";
        customerRepository.save(new Customer("From DB", email, 30, "MALE", "encoded-password"));

        redis.stop();

        // GET falls back to PostgreSQL and PUT failures do not break the read.
        Customer customer = getCustomerPort.getCustomerByEmail(email);
        assertThat(customer.getName()).isEqualTo("From DB");

        // EVICT failures do not break the write.
        registerCustomerPort.execute("New", "new@example.com", 25, "FEMALE", "Password123!");
        assertThat(customerRepository.existsCustomerByEmail("new@example.com")).isTrue();
    }
}
