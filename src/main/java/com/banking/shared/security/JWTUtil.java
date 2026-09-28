package com.banking.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JWTUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigninKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(String subject) {
        return issueToken(subject, Map.of(), 15, ChronoUnit.MINUTES);
    }

    public String issueToken(String subject, Map<String, Object> claims) {
        return issueToken(subject, claims, 15, ChronoUnit.MINUTES);
    }

    public String issueToken(String subject, Map<String, Object> claims, long duration, ChronoUnit unit) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer("banking-api")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(duration, unit)))
                .signWith(getSigninKey())
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String jwt, String username) {
        String subject = getSubject(jwt);
        return subject.equals(username) && !isTokenExpired(jwt);
    }

    private boolean isTokenExpired(String jwt) {
        Date today = Date.from(Instant.now());
        return getClaims(jwt).getExpiration().before(today);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = getClaims(token);
        List<String> roles = (List<String>) claims.get("roles");
        return roles != null ? roles : List.of();
    }

    public String generateTestToken(String subject, List<String> roles) {
        return issueToken(subject, Map.of("roles", roles), 1, ChronoUnit.HOURS);
    }
}