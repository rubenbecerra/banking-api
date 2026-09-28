package com.banking.shared.config;

import com.banking.shared.security.IdempotencyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    public WebConfig(IdempotencyInterceptor idempotencyInterceptor) {
        this.idempotencyInterceptor = idempotencyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns(
                        "/api/v1/transactions/deposit",
                        "/api/v1/transactions/admin/deposit",
                        "/api/v1/transactions/withdraw",
                        "/api/v1/transactions/admin/withdraw",
                        "/api/v1/transactions/transfer"
                );
    }
}