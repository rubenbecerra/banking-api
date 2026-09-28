package com.banking.customer;


import com.banking.customers.domain.model.Customer;
import com.banking.customers.domain.model.Role;
import com.banking.customers.infrastructure.rest.CustomerDTO;
import com.banking.customers.infrastructure.rest.CustomerDTOMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CustomerDTOMapperTest {
    private CustomerDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new CustomerDTOMapper();
    }
    @Test
    void shouldMapCustomerToCustomerDTO() {
        //Given a customer
        Customer customer = new Customer(
                "alex",
                "alex@gmail.com",
                25,
                "Male",
                "123",
                Role.ROLE_USER
        );
        // Create the customer DTO
        CustomerDTO actual = underTest.apply(customer);
        // Create the customer with correct data
        CustomerDTO expected = new CustomerDTO(
                null,
                "alex",
                "alex@gmail.com",
                25,
                "Male"

        );
        //Then comparing the objects must be equals
        assertThat(actual).isEqualTo(expected);
    }
}
