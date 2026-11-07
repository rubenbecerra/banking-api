package com.banking.shared.security;

import com.banking.customer.AbstractTestContainers;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class JwtAuthenticationFilterIntegrationTest extends AbstractTestContainers {

    @LocalServerPort
    private int port;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private ServletContext servletContext;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void invalidTokenReturns401() {
        HttpClientErrorException.Unauthorized exception = callProtectedEndpoint("this-is-not-a-jwt");

        assertThat(exception).isNotNull();
        assertThat(exception.getResponseBodyAsString()).contains("Invalid or expired token");
    }

    @Test
    void expiredTokenReturns401() {
        String expiredToken = jwtUtil.issueToken(
                "user@example.com",
                Map.of("roles", List.of("ROLE_USER")),
                -5,
                ChronoUnit.MINUTES);

        HttpClientErrorException.Unauthorized exception = callProtectedEndpoint(expiredToken);

        assertThat(exception).isNotNull();
        assertThat(exception.getResponseBodyAsString()).contains("Invalid or expired token");
    }

    @Test
    void missingTokenReturns401() {
        HttpClientErrorException.Unauthorized exception = callProtectedEndpoint(null);

        assertThat(exception).isNotNull();
        assertThat(exception.getResponseBodyAsString()).contains("Authentication required");
    }

    @Test
    void jwtFilterRunsOnlyInsideTheSecurityChain() {
        assertThat(servletContext.getFilterRegistrations().values())
                .noneMatch(registration -> JWTAuthenticationFilter.class.getName()
                        .equals(registration.getClassName()));
    }

    @Test
    void actuatorIgnoresInvalidToken() {
        ResponseEntity<String> response = restClient.get()
                .uri("/actuator/health")
                .header(HttpHeaders.COOKIE, "accessToken=this-is-not-a-jwt")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpClientErrorException.Unauthorized callProtectedEndpoint(String token) {
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri("/api/v1/customers/me");

        if (token != null) {
            request = request.header(HttpHeaders.COOKIE, "accessToken=" + token);
        }

        RestClient.RequestHeadersSpec<?> finalRequest = request;
        return catchThrowableOfType(
                () -> finalRequest.retrieve().toBodilessEntity(),
                HttpClientErrorException.Unauthorized.class);
    }
}
