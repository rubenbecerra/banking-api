package com.banking.customer;


import com.banking.auth.application.AuthenticationUseCase;
import com.banking.auth.infrastructure.rest.AuthenticationRequest;
import com.banking.auth.infrastructure.rest.AuthenticationResponse;
import com.banking.auth.infrastructure.security.RedisTokenRepository;
import com.banking.shared.security.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private RedisTokenRepository redisTokenRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticationUseCase authenticationUseCase;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new User("ruben@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void login_shouldReturnTokensAndSaveInRedis() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(jwtUtil.issueToken(eq("ruben@gmail.com"), anyMap(), eq(15L), eq(ChronoUnit.MINUTES))).thenReturn("mock-access-token");
        when(jwtUtil.issueToken(eq("ruben@gmail.com"), anyMap(), eq(7L), eq(ChronoUnit.DAYS))).thenReturn("mock-refresh-token");

        AuthenticationResponse response = authenticationUseCase.login(new AuthenticationRequest("ruben@gmail.com", "123"));

        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals("ruben@gmail.com", response.name());
        assertEquals("ROLE_USER", response.role());

        verify(redisTokenRepository, times(1)).save(eq("ruben@gmail.com"), eq("mock-refresh-token"), any(Duration.class));
    }

    @Test
    void refreshToken_shouldRotateTokensSuccessfully() {
        String oldRefreshToken = "old-refresh-token";
        when(jwtUtil.getSubject(oldRefreshToken)).thenReturn("ruben@gmail.com");
        when(redisTokenRepository.get("ruben@gmail.com")).thenReturn(oldRefreshToken);
        when(userDetailsService.loadUserByUsername("ruben@gmail.com")).thenReturn(userDetails);

        when(jwtUtil.issueToken(eq("ruben@gmail.com"), anyMap(), eq(15L), eq(ChronoUnit.MINUTES))).thenReturn("new-access-token");
        when(jwtUtil.issueToken(eq("ruben@gmail.com"), anyMap(), eq(7L), eq(ChronoUnit.DAYS))).thenReturn("new-refresh-token");


        AuthenticationResponse response = authenticationUseCase.refreshToken(oldRefreshToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());

        verify(redisTokenRepository, times(1)).save(eq("ruben@gmail.com"), eq("new-refresh-token"), any(Duration.class));
    }

    @Test
    void refreshToken_shouldThrowExceptionWhenTokenMismatched() {
        String invalidToken = "fake-refresh-token";
        when(jwtUtil.getSubject(invalidToken)).thenReturn("ruben@gmail.com");
        when(redisTokenRepository.get("ruben@gmail.com")).thenReturn("different-stored-token");

        assertThrows(RuntimeException.class, () -> {
            authenticationUseCase.refreshToken(invalidToken);
        });

        verify(redisTokenRepository, never()).save(anyString(), anyString(), any());
    }
}