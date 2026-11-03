package com.banking.shared.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingCacheErrorHandlerTest {

    private final Cache cache = mock(Cache.class);
    private final CacheErrorHandler handler = new LoggingCacheErrorHandler();

    @Test
    void shouldSwallowEveryCacheError() {
        when(cache.getName()).thenReturn("customer_by_email");
        RuntimeException failure = new RuntimeException("Redis is down");

        assertThatCode(() -> handler.handleCacheGetError(failure, cache, "a@b.com"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(failure, cache, "a@b.com", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(failure, cache, "a@b.com"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(failure, cache))
                .doesNotThrowAnyException();
    }
}
