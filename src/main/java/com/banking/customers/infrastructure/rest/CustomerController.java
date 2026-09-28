package com.banking.customers.infrastructure.rest;

import com.banking.customers.application.usecase.DeleteCustomerPort;
import com.banking.customers.application.usecase.GetCustomerPort;
import com.banking.customers.application.usecase.RegisterCustomerPort;
import com.banking.customers.application.usecase.UpdateCustomerPort;
import com.banking.customers.domain.model.Customer;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/customers")
public class CustomerController {

    private final RegisterCustomerPort registerCustomerPort;
    private final GetCustomerPort getCustomerPort;
    private final UpdateCustomerPort updateCustomerPort;
    private final DeleteCustomerPort deleteCustomerPort;
    private final CustomerDTOMapper customerDTOMapper;

    public CustomerController(
            RegisterCustomerPort registerCustomerPort,
            GetCustomerPort getCustomerPort,
            UpdateCustomerPort updateCustomerPort,
            DeleteCustomerPort deleteCustomerPort,
            CustomerDTOMapper customerDTOMapper
    ) {
        this.registerCustomerPort = registerCustomerPort;
        this.getCustomerPort = getCustomerPort;
        this.updateCustomerPort = updateCustomerPort;
        this.deleteCustomerPort = deleteCustomerPort;
        this.customerDTOMapper = customerDTOMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<CustomerDTO> getCustomersDTO() {
        return getCustomerPort.getAllCustomers()
                .stream()
                .map(customerDTOMapper)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("{id}")
    public CustomerDTO getCustomerDTO(@PathVariable("id") Integer id) {
        Customer customer = getCustomerPort.getCustomerById(id);
        return customerDTOMapper.apply(customer);
    }

    @GetMapping("me")
    public CustomerDTO getAuthenticatedCustomer(Authentication authentication) {
        String email = authentication.getName();
        Customer customer = getCustomerPort.getCustomerByEmail(email);
        return customerDTOMapper.apply(customer);
    }

    @PostMapping
    public void registerCustomer(@Valid @RequestBody CustomerRegistrationRequest request) {
        registerCustomerPort.execute(
                request.name(),
                request.email(),
                request.age(),
                request.gender(),
                request.password()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("{id}")
    public void deleteCustomer(@PathVariable("id") Integer id) {
        deleteCustomerPort.deleteById(id);
    }

    @DeleteMapping("me")
    public void deleteAuthenticatedCustomer(Authentication authentication) {
        String email = authentication.getName();
        deleteCustomerPort.deleteByEmail(email);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("{id}")
    public void updateCustomer(
            @PathVariable("id") Integer id,
            @RequestBody CustomerUpdateRequest request) {
        updateCustomerPort.update(id, request.name(), request.age(), request.gender());
    }

    @PutMapping("me")
    public void updateAuthenticatedCustomer(Authentication authentication,
                                            @RequestBody CustomerUpdateRequest request) {
        String email = authentication.getName();
        updateCustomerPort.updateByEmail(email, request.name(), request.age(), request.gender());
    }
}