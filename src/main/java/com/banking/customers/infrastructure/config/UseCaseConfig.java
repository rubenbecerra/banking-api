package com.banking.customers.infrastructure.config;


import com.banking.auth.application.AuthenticationUseCase;
import com.banking.auth.infrastructure.security.RedisTokenRepository;
import com.banking.customers.application.decorator.CachedDeleteCustomerUseCaseDecorator;
import com.banking.customers.application.decorator.CachedGetCustomerUseCaseDecorator;
import com.banking.customers.application.decorator.CachedRegisterCustomerUseCaseDecorator;
import com.banking.customers.application.decorator.CachedUpdateCustomerUseCaseDecorator;
import com.banking.customers.application.decorator.transactional.UpdateCustomerUseCaseTransactionalDecorator;
import com.banking.customers.application.usecase.DeleteCustomerPort;
import com.banking.customers.application.usecase.DeleteCustomerUseCase;
import com.banking.customers.application.usecase.GetCustomerPort;
import com.banking.customers.application.usecase.GetCustomerUseCase;
import com.banking.customers.application.usecase.RegisterCustomerPort;
import com.banking.customers.application.usecase.RegisterCustomerUseCase;
import com.banking.customers.application.usecase.UpdateCustomerPort;
import com.banking.customers.application.usecase.UpdateCustomerUseCase;
import com.banking.customers.domain.repository.CustomerRepository;
import com.banking.shared.security.JWTUtil;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class UseCaseConfig {

    @Bean
    public DeleteCustomerPort deleteCustomerPort(CustomerRepository customerRepository) {
        return new CachedDeleteCustomerUseCaseDecorator(
                new DeleteCustomerUseCase(customerRepository));
    }

    @Bean
    public RegisterCustomerPort registerCustomerPort(CustomerRepository customerRepository,
                                                     PasswordEncoder passwordEncoder) {
        return new CachedRegisterCustomerUseCaseDecorator(
                new RegisterCustomerUseCase(customerRepository, passwordEncoder));
    }

    @Bean
    public GetCustomerPort getCustomerPort(CustomerRepository customerRepository) {
        return new CachedGetCustomerUseCaseDecorator(
                new GetCustomerUseCase(customerRepository));
    }

    @Bean
    public UpdateCustomerPort updateCustomerPort(CustomerRepository customerRepository,
                                                 TransactionTemplate transactionTemplate,
                                                 CacheManager cacheManager) {
        UpdateCustomerUseCase useCase = new UpdateCustomerUseCase(customerRepository);
        UpdateCustomerPort transactional =
                new UpdateCustomerUseCaseTransactionalDecorator(useCase, transactionTemplate);
        return new CachedUpdateCustomerUseCaseDecorator(transactional, cacheManager);
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
