package com.banking.auth.infrastructure.rest;

import com.banking.auth.application.AuthenticationUseCase;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationUseCase authenticationService;

    public AuthenticationController(AuthenticationUseCase authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody AuthenticationRequest request,
            HttpServletResponse response
    ) {
        AuthenticationResponse authResult = authenticationService.login(request);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", authResult.accessToken())
                .httpOnly(true)
                .secure(false) // Change to "true" in production
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("Strict")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", authResult.refreshToken())
                .httpOnly(true)
                .secure(false) // Same here
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok(authResult);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse response
    ) {
        AuthenticationResponse authResult = authenticationService.refreshToken(request.refreshToken());

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", authResult.accessToken())
                .httpOnly(true)
                .secure(false) // And here
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        return ResponseEntity.ok(authResult);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();

            authenticationService.logout(username);
        }

        ResponseCookie deleteAccess = ResponseCookie.from("accessToken", "").httpOnly(true).path("/").maxAge(0).build();
        ResponseCookie deleteRefresh = ResponseCookie.from("refreshToken", "").httpOnly(true).path("/").maxAge(0).build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());

        return ResponseEntity.ok().build();
    }
}