package com.banking.shared.config;

import com.banking.customer.AbstractTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationTest extends AbstractTestContainers {

    @Autowired
    private Environment environment;

    @Test
    void defaultProfileUsesProductionSafeDefaults() {
        assertThat(environment.getActiveProfiles()).isEmpty();

        assertThat(environment.getProperty("spring.web.error.include-stacktrace")).isEqualTo("never");
        assertThat(environment.getProperty("spring.web.error.include-message")).isEqualTo("never");
        assertThat(environment.getProperty("spring.web.error.include-binding-errors")).isEqualTo("never");

        assertThat(environment.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(environment.getProperty("management.endpoint.health.show-components")).isEqualTo("never");
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,prometheus");

        assertThat(environment.getProperty("spring.jpa.show-sql", Boolean.class, false)).isFalse();
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class, true)).isFalse();
    }
}
