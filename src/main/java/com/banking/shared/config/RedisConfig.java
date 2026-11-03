package com.banking.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer
                .builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.banking.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("java.math.")
                        .allowIfSubType("java.lang.")
                        .build())
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          RedisCacheConfiguration defaultCacheConfiguration) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfiguration)
                .build();
    }

}
