package com.banking.auth.application;


import com.banking.auth.infrastructure.rest.AuthenticationRequest;
import com.banking.auth.infrastructure.rest.AuthenticationResponse;
import com.banking.auth.infrastructure.security.RedisTokenRepository;
import com.banking.shared.security.JWTUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;


public class AuthenticationUseCase {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RedisTokenRepository redisTokenRepository;
    private final UserDetailsService userDetailsService;

    public AuthenticationUseCase(AuthenticationManager authenticationManager,
                                 JWTUtil jwtUtil,
                                 RedisTokenRepository redisTokenRepository,
                                 UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.redisTokenRepository = redisTokenRepository;
        this.userDetailsService = userDetailsService;
    }

    public AuthenticationResponse login(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();

        assert principal != null;
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtUtil.issueToken(principal.getUsername(),
                Map.of("roles", roles), 15, ChronoUnit.MINUTES);

        String refreshToken = jwtUtil.issueToken(principal.getUsername(),
                Map.of(), 7, ChronoUnit.DAYS);

        redisTokenRepository.save(principal.getUsername(), refreshToken, Duration.ofDays(7));

        String firstRole = roles.isEmpty() ? "" : roles.getFirst();

        return new AuthenticationResponse(accessToken, refreshToken, principal.getUsername(), firstRole);
    }

    public AuthenticationResponse refreshToken(String refreshTokenRequest) {
        String username = jwtUtil.getSubject(refreshTokenRequest);

        String storedToken = redisTokenRepository.get(username);
        if (storedToken == null || !storedToken.equals(refreshTokenRequest)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String newAccessToken = jwtUtil.issueToken(username, Map.of("roles", roles), 15, ChronoUnit.MINUTES);

        String newRefreshToken = jwtUtil.issueToken(username, Map.of(), 7, ChronoUnit.DAYS);

        redisTokenRepository.save(username, newRefreshToken, Duration.ofDays(7));

        String firstRole = roles.isEmpty() ? "" : roles.getFirst();

        return new AuthenticationResponse(newAccessToken, newRefreshToken, username, firstRole);
    }
    public void logout(String username) {
        redisTokenRepository.delete(username);
    }
}