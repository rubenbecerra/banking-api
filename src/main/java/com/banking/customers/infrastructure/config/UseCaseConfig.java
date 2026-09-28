package com.banking.customers.infrastructure.config;


import com.banking.auth.application.AuthenticationUseCase;
import com.banking.auth.infrastructure.security.RedisTokenRepository;
import com.banking.customers.application.usecase.DeleteCustomerUseCase;
import com.banking.customers.application.usecase.GetCustomerUseCase;
import com.banking.customers.application.usecase.RegisterCustomerUseCase;
import com.banking.customers.application.usecase.UpdateCustomerUseCase;
import com.banking.customers.domain.repository.CustomerRepository;
import com.banking.shared.security.JWTUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UseCaseConfig {

    @Bean
    public DeleteCustomerUseCase deleteCustomerUseCase(CustomerRepository customerRepository) {
        return new DeleteCustomerUseCase(customerRepository);
    }

    @Bean
    public RegisterCustomerUseCase registerCustomerUseCase(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        return new RegisterCustomerUseCase(customerRepository, passwordEncoder);
    }

    @Bean
    public GetCustomerUseCase getCustomerUseCase(CustomerRepository customerRepository) {
        return new GetCustomerUseCase(customerRepository);
    }

    @Bean
    public UpdateCustomerUseCase updateCustomerUseCase(CustomerRepository customerRepository) {
        return new UpdateCustomerUseCase(customerRepository);
    }

    @Bean
    public AuthenticationUseCase authenticationUseCase(AuthenticationManager authenticationManager,
                                                       JWTUtil jwtUtil,
                                                       RedisTokenRepository redisTokenRepository,
                                                       UserDetailsService userDetailsService) {
        return new AuthenticationUseCase(authenticationManager,
                 jwtUtil,
                 redisTokenRepository,
                 userDetailsService);
    }
}