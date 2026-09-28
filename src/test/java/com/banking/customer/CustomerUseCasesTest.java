package com.banking.customer;


import com.banking.customers.application.usecase.DeleteCustomerUseCase;
import com.banking.customers.application.usecase.GetCustomerUseCase;
import com.banking.customers.application.usecase.RegisterCustomerUseCase;
import com.banking.customers.application.usecase.UpdateCustomerUseCase;
import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.model.Role;
import com.banking.customers.domain.repository.CustomerRepository;
import com.banking.customers.infrastructure.rest.CustomerRegistrationRequest;
import com.banking.customers.infrastructure.rest.CustomerUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerUseCasesTest
{

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private GetCustomerUseCase getUseCase;
    private RegisterCustomerUseCase registerUseCase;
    private UpdateCustomerUseCase updateUseCase;
    private DeleteCustomerUseCase deleteUseCase;

    @BeforeEach
    void setup() {
        getUseCase = new GetCustomerUseCase(customerRepository);
        registerUseCase = new RegisterCustomerUseCase(customerRepository, passwordEncoder);
        updateUseCase = new UpdateCustomerUseCase(customerRepository);
        deleteUseCase = new DeleteCustomerUseCase(customerRepository);
    }

    @Test
    void canGetAllCustomers() {
        getUseCase.getAllCustomers();
        verify(customerRepository).findAll();
    }

    @Test
    void canGetCustomerById() {
        int id = 10;
        Customer customer = new Customer("alex", "alex@gmail.com", 25, "Male", "123", Role.ROLE_USER);

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer actual = getUseCase.getCustomerById(id);
        assertThat(actual).isEqualTo(customer);
    }

    @Test
    void willThrowWhenGetCustomerByIdReturnsEmptyOptional() {
        int id = 10;
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getUseCase.getCustomerById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Customer with ID " + id + " not found");
    }

    @Test
    void willThrowWhenDeleteCustomerDoesNotExist() {
        int id = 10;
        when(customerRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> deleteUseCase.deleteById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Client with ID " + id + " doesn't exist");

        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void canUpdateACustomer() {
        int id = 10;
        Customer customer = new Customer("alex", "alex@gmail.com", 25, "Male", "123", Role.ROLE_USER);
        CustomerUpdateRequest update = new CustomerUpdateRequest("alexandre", 22, "Male");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        updateUseCase.update(id, update.name(), update.age(), update.gender());

        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerArgumentCaptor.capture());

        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertThat(capturedCustomer.getName()).isEqualTo(update.name());
        assertThat(capturedCustomer.getAge()).isEqualTo(update.age());
        assertThat(capturedCustomer.getGender()).isEqualTo(update.gender());
    }

    @Test
    void willThrowWhenUpdateHasNoChanges() {
        int id = 10;
        Customer customer = new Customer("alex", "alex@gmail.com", 20, "Male", "123", Role.ROLE_USER);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        CustomerUpdateRequest update = new CustomerUpdateRequest("alex", 20, "Male");

        assertThatThrownBy(() -> updateUseCase.update(id, update.name(), update.age(), update.gender()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No data changes found");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void canAddCustomer() {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest("alex", "alex@gmail.com", 20, "Male", "123");
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_123");

        registerUseCase.execute(request.name(), request.email(), request.age(), request.gender(), request.password());

        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerArgumentCaptor.capture());

        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertThat(capturedCustomer.getId()).isNull();
        assertThat(capturedCustomer.getName()).isEqualTo(request.name());
        assertThat(capturedCustomer.getEmail()).isEqualTo(request.email());
        assertThat(capturedCustomer.getAge()).isEqualTo(request.age());
        assertThat(capturedCustomer.getGender()).isEqualTo(request.gender());
        assertThat(capturedCustomer.getPassword()).isEqualTo("encoded_123");
    }

    @Test
    void canDeleteCustomer() {
        int id = 10;
        when(customerRepository.existsById(id)).thenReturn(true);

        deleteUseCase.deleteById(id);

        verify(customerRepository).deleteById(10);
    }

    @Test
    void willThrowWhenCreateUserWithEmailInUse() {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest("alex", "alex@gmail.com", 20, "Male", "123");

        when(customerRepository.existsCustomerByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> registerUseCase.execute(request.name(), request.email(), request.age(), request.gender(), request.password()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email already taken");
    }
}