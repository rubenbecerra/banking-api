package com.banking.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String HEADER_KEY = "X-Idempotency-Key";
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");
    private static final String INVALID_KEY_MESSAGE =
            "X-Idempotency-Key must be between 8 and 128 characters and contain only letters, "
                    + "digits, '.', '_', ':' or '-'";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String idempotencyKey = request.getHeader(HEADER_KEY);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }

        if (!KEY_PATTERN.matcher(idempotencyKey).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"status":400,"error":"Bad Request","message":"%s"}
                    """.formatted(INVALID_KEY_MESSAGE));
            return false;
        }

        String user = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "anonymous";
        IdempotencyContextHolder.setCurrentIdempotencyKey(idempotencyKey, user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        IdempotencyContextHolder.clear();
    }
}
