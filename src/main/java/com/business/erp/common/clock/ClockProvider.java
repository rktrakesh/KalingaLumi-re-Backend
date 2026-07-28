package com.business.erp.common.clock;

import java.time.LocalDate;

/**
 * Abstraction over "what is today" for anything that needs deterministic, testable date
 * comparisons — most importantly the payroll engine. No engine class should call
 * {@code LocalDate.now()} directly; inject this instead so tests can substitute a fixed
 * {@link java.time.Clock} and assert exact boundary behaviour (e.g. "generate exactly on
 * the last day of the period", "generate exactly one day after").
 */
public interface ClockProvider {
    LocalDate today();
}