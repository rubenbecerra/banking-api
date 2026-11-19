package com.banking.shared.outbox.infrastructure.config;

import com.banking.shared.outbox.application.OutboxPublisher;
import com.banking.shared.outbox.application.OutboxRelay;
import com.banking.shared.outbox.domain.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class OutboxConfiguration {

    @Bean
    public OutboxRelay outboxRelay(OutboxEventRepository repository,
                                   OutboxPublisher publisher,
                                   Clock clock,
                                   @Value("${outbox.relay.lease-seconds:30}") long leaseSeconds) {
        return new OutboxRelay(repository,publisher,clock, Duration.ofSeconds(leaseSeconds));
    }
}
