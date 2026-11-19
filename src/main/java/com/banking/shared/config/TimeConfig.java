package com.banking.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * One application clock for everything that measures time: the idempotency
 * windows, the relay lease and both purge jobs.
 *
 * One bean, two reasons: every reader of "now" sees the same instants, and a
 * test that overrides it overrides it everywhere. Comparing timestamps written
 * by one clock against another clock (for example the database clock) only
 * works while both machines agree.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
