package com.banking.customer;

import com.banking.customers.application.decorator.CachedDeleteCustomerUseCaseDecorator;
import com.banking.customers.application.decorator.CachedGetCustomerUseCaseDecorator;
import com.banking.customers.application.decorator.CachedRegisterCustomerUseCaseDecorator;
import com.banking.customers.application.decorator.CachedUpdateCustomerUseCaseDecorator;
import com.banking.customers.application.usecase.DeleteCustomerPort;
import com.banking.customers.application.usecase.GetCustomerPort;
import com.banking.customers.application.usecase.RegisterCustomerPort;
import com.banking.customers.application.usecase.UpdateCustomerPort;
import com.banking.customers.domain.model.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CachedCustomerUseCasesTest {

    @Mock
    private UpdateCustomerPort updateTarget;

    @InjectMocks
    private CachedUpdateCustomerUseCaseDecorator updateDecorator;

    @Test
    void shouldDelegateUpdate() {
        updateDecorator.update(1, "Alice", 30, "Female");
        verify(updateTarget).update(1, "Alice", 30, "Female");
    }

    @Test
    void shouldDelegateUpdateByEmail() {
        updateDecorator.updateByEmail("alice@example.com", "Alice", 30, "Female");
        verify(updateTarget).updateByEmail("alice@example.com", "Alice", 30, "Female");
    }

    @Mock
    private DeleteCustomerPort deleteTarget;

    @InjectMocks
    private CachedDeleteCustomerUseCaseDecorator deleteDecorator;

    @Test
    void shouldDelegateDeleteById() {
        deleteDecorator.deleteById(1);
        verify(deleteTarget).deleteById(1);
    }

    @Test
    void shouldDelegateDeleteByEmail() {
        deleteDecorator.deleteByEmail("test@example.com");
        verify(deleteTarget).deleteByEmail("test@example.com");
    }

    @Mock
    private RegisterCustomerPort registerTarget;

    @InjectMocks
    private CachedRegisterCustomerUseCaseDecorator registerDecorator;

    @Test
    void shouldDelegateExecute() {
        registerDecorator.execute("Bob", "bob@example.com", 25, "Male", "password123");
        verify(registerTarget).execute("Bob", "bob@example.com", 25, "Male", "password123");
    }

    @Mock
    private GetCustomerPort getTarget;

    @InjectMocks
    private CachedGetCustomerUseCaseDecorator getDecorator;

    @Test
    void shouldDelegateGetCustomerByEmail() {
        Customer mockCustomer = new Customer();
        when(getTarget.getCustomerByEmail("test@example.com")).thenReturn(mockCustomer);

        Customer result = getDecorator.getCustomerByEmail("test@example.com");

        assertNotNull(result);
        verify(getTarget).getCustomerByEmail("test@example.com");
    }
}