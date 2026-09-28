package com.banking.customer;

import com.banking.Main;
import com.banking.auth.infrastructure.rest.AuthenticationRequest;
import com.banking.auth.infrastructure.rest.AuthenticationResponse;
import com.banking.auth.infrastructure.rest.RefreshTokenRequest;
import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.model.Role;
import com.banking.customers.domain.repository.CustomerRepository;
import com.banking.customers.infrastructure.rest.CustomerDTO;
import com.banking.customers.infrastructure.rest.CustomerRegistrationRequest;
import com.banking.customers.infrastructure.rest.CustomerUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class CustomerIntegrationTest extends AbstractTestcontainersTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    private RestClient restClient;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Must register a new client successfully")
    void shouldRegisterCustomer() {
        String email = "alex-" + UUID.randomUUID() + "@example.com";
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "Alex",
                email,
                25,
                "Male",
                "Password123!"
        );

        ResponseEntity<Void> response = restClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(customerRepository.existsCustomerByEmail(email)).isTrue();
    }
    @Test
    @DisplayName("Must retrieve all the customers")
    void shouldGetAllCustomers() {
        String email1 = "user1-" + UUID.randomUUID() + "@gmail.com";
        String email2 = "user2-" + UUID.randomUUID() + "@gmail.com";


        Customer admin = new Customer(
                "admin",
                email1,
                25,
                "MALE",
                passwordEncoder.encode("123"),
                Role.ROLE_ADMIN
        );
        customerRepository.save(admin);


        CustomerRegistrationRequest request2 = new CustomerRegistrationRequest(
                "user2",
                email2,
                29,
                "MALE",
                "1234"
        );

        AuthenticationRequest authReq = new AuthenticationRequest(
                email1,
                "123"
        );

        ResponseEntity<Void> loginResponse = restClient.post()
                .uri("/api/v1/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(authReq)
                .retrieve()
                .toBodilessEntity();

        String cookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        restClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .retrieve()
                .toBodilessEntity();


        List<CustomerDTO> customers = restClient.get()
                .uri("api/v1/customers")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CustomerDTO>>() {
                });
        assertThat(customers).isNotNull();
        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(CustomerDTO::email)
                .containsExactlyInAnyOrder(email1,email2);
    }
    @Test
    @DisplayName("Should retrieve the authenticated customer")
    void shouldGetAuthenticatedCustomer() {
        String email1 = "user1-" + UUID.randomUUID() + "@gmail.com";
        String email2 = "user2-" + UUID.randomUUID() + "@gmail.com";

        CustomerRegistrationRequest request1 =  new CustomerRegistrationRequest(
                "user1",
                email1,
                25,
                "MALE",
                "123"
        );

        CustomerRegistrationRequest request2 = new CustomerRegistrationRequest(
                "user2",
                email2,
                29,
                "MALE",
                "1234"
        );

        restClient.post()
                .uri("api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request1)
                .retrieve()
                .toBodilessEntity();

        AuthenticationRequest authReq = new AuthenticationRequest(
                email1,
                "123"
        );
        ResponseEntity<Void> loginResponse = restClient.post()
                .uri("api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(authReq)
                .retrieve()
                .toBodilessEntity();

        String cookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        restClient.post()
                .uri("api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .retrieve().toBodilessEntity();


        CustomerDTO customer = restClient.get()
                .uri("api/v1/customers/me")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(CustomerDTO.class);
        assertThat(customer).isNotNull();
        assertThat(customer.name()).isEqualTo("user1");
        assertThat(customer.email()).isEqualTo(email1);
        assertThat(customer.age()).isEqualTo(25);
        assertThat(customer.gender()).isEqualTo("MALE");

    }
    @Test
    @DisplayName("Should delete the authenticated user")
    void shouldDeleteAuthenticatedUser() {
        String email1 = "user1-" + UUID.randomUUID() + "@gmail.com";
        String email2 = "user2-" + UUID.randomUUID() + "@gmail.com";

        CustomerRegistrationRequest request1 = new CustomerRegistrationRequest(
                "user1",
                email1,
                26,
                "FEMALE",
                "123"
        );
        CustomerRegistrationRequest request2 = new CustomerRegistrationRequest(
                "user2",
                email2,
                28,
                "MALE",
                "1234"
        );

        restClient.post()
                .uri("api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request1)
                .retrieve()
                .toBodilessEntity();

        AuthenticationRequest authReq = new AuthenticationRequest(
                email1,
                "123"
        );
        ResponseEntity<Void> loginResponse = restClient.post()
                .uri("api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(authReq)
                .retrieve().toBodilessEntity();

        String cookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        restClient.post()
                .uri("api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .retrieve().toBodilessEntity();


        ResponseEntity<Void> deleteResponse = restClient.delete()
                .uri("api/v1/customers/me")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().toBodilessEntity();

        assertThat(deleteResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(customerRepository.existsCustomerByEmail(email1)).isFalse();
        assertThatThrownBy(() -> restClient.get()
                .uri("api/v1/customers/me")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .retrieve().body(CustomerDTO.class)
        ).isInstanceOf(HttpClientErrorException.NotFound.class);

    }
    @Test
    @DisplayName("Should update the user's mail")
    void shouldUpdateName() {
        String originalNAme = "user1";
        String email = "user1@gmail.com";

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "user1",
                email,
                24,
                "MALE",
                "123"
        );
        restClient.post()
                .uri("api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        AuthenticationRequest authReq = new AuthenticationRequest(
                email,
                "123"
        );
        ResponseEntity<Void> loginResponse = restClient.post()
                .uri("api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(authReq)
                .retrieve().toBodilessEntity();

        String cookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        String updatedName = "user";
        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                updatedName,
                32,
                "MALE"

        );
        ResponseEntity<Void> updateResponse = restClient.put()
                .uri("api/v1/customers/me")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .retrieve().toBodilessEntity();

        assertThat(updateResponse.getStatusCode().value()).isEqualTo(200);

        Customer updatedInDb = customerRepository.findByEmail(email).orElseThrow();
        assertThat(updatedInDb.getName()).isEqualTo(updatedName);
        assertThat(updatedInDb.getEmail()).isEqualTo(email);
        assertThat(updatedInDb.getAge()).isEqualTo(32);
    }
    @Test
    @DisplayName("Should login successfully and refresh tokens using Redis")
    void shouldLoginAndRefreshTokenSuccessfully() throws InterruptedException {
        String email = "auth-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        CustomerRegistrationRequest registerRequest = new CustomerRegistrationRequest(
                "AuthUser",
                email,
                30,
                "MALE",
                password
        );

        restClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .retrieve()
                .toBodilessEntity();

        AuthenticationRequest authReq = new AuthenticationRequest(email, password);

        ResponseEntity<AuthenticationResponse> loginResponse = restClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(authReq)
                .retrieve()
                .toEntity(AuthenticationResponse.class);

        assertThat(loginResponse.getStatusCode().value()).isEqualTo(200);
        AuthenticationResponse authBody = loginResponse.getBody();
        assertThat(authBody).isNotNull();
        assertThat(authBody.accessToken()).isNotBlank();
        assertThat(authBody.refreshToken()).isNotBlank();


        Thread.sleep(1000);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(authBody.refreshToken());

        ResponseEntity<AuthenticationResponse> refreshResponse = restClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(refreshRequest)
                .retrieve()
                .toEntity(AuthenticationResponse.class);

        assertThat(refreshResponse.getStatusCode().value()).isEqualTo(200);
        AuthenticationResponse refreshBody = refreshResponse.getBody();
        assertThat(refreshBody).isNotNull();
        assertThat(refreshBody.accessToken()).isNotBlank();
        assertThat(refreshBody.refreshToken()).isNotBlank();

        assertThat(refreshBody.accessToken()).isNotEqualTo(authBody.accessToken());
        assertThat(refreshBody.refreshToken()).isNotEqualTo(authBody.refreshToken());
    }
}