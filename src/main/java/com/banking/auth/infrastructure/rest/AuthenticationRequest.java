package com.banking.auth.infrastructure.rest;

public record AuthenticationRequest(
        String username,
        String password
) {

}
