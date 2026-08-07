package com.business.erp.common.clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;

/** Provides the system default {@link Clock} as a bean, so it can be swapped in tests via a test configuration. */
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Kolkata}") String timezone) {
        return Clock.system(java.time.ZoneId.of(timezone));
    }
}
