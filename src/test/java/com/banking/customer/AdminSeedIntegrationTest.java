package com.banking.customer;

import com.banking.Main;
import com.banking.auth.infrastructure.rest.AuthenticationRequest;
import com.banking.auth.infrastructure.rest.AuthenticationResponse;
import com.banking.customers.domain.model.Role;
import com.banking.customers.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dedicated context so other tests cannot delete the Flyway-seeded admin row.
 */
@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "test.context=admin-seed")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminSeedIntegrationTest {

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

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void seededAdminCanLogin() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        ResponseEntity<AuthenticationResponse> response = restClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthenticationRequest("admin@bank.com", "Admin123!"))
                .retrieve()
                .toEntity(AuthenticationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("admin@bank.com");
        assertThat(response.getBody().role()).isEqualTo("ROLE_ADMIN");
        assertThat(response.getBody().accessToken()).isNotBlank();

        assertThat(customerRepository.findByEmail("admin@bank.com"))
                .isPresent()
                .get()
                .satisfies(customer -> assertThat(customer.getRole()).isEqualTo(Role.ROLE_ADMIN));
    }
}
