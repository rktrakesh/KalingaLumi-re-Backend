package com.business.erp.common.clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Production {@link ClockProvider}, backed by the {@link Clock} bean defined in
 * {@link ClockConfig}. Unit tests can bypass Spring entirely and construct this with a
 * fixed {@code Clock.fixed(...)} for deterministic date-boundary testing.
 */
@Component
@RequiredArgsConstructor
public class SystemClockProvider implements ClockProvider {

    private final Clock clock;

    @Override
    public LocalDate today() {
        return LocalDate.now(clock);
    }
}