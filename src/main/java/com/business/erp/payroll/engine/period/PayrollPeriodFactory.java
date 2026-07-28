package com.business.erp.payroll.engine.period;

import com.business.erp.common.clock.ClockProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The single place {@link PayrollPeriod} instances get created. No service should ever
 * construct a {@code PayrollPeriod} directly — always go through this factory, so every
 * future payroll frequency (weekly, fortnightly, contractor) has exactly one place to
 * add a new creation method rather than every caller inventing its own date math.
 * <p>
 * Lives in the same package as {@link PayrollPeriod} specifically so it is the only
 * class (besides {@code PayrollPeriod} itself, for {@code nextPeriod()}/{@code previousPeriod()})
 * that can reach its package-private constructor.
 */
@Component
@RequiredArgsConstructor
public class PayrollPeriodFactory {

    private final ClockProvider clockProvider;

    /** The calendar-month period containing today (via the injected clock, never {@code LocalDate.now()} directly). */
    public PayrollPeriod current() {
        LocalDate today = clockProvider.today();
        return of(today.getMonthValue(), today.getYear());
    }

    /** A calendar-month period for the given month/year. */
    public PayrollPeriod of(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        return of(ym.atDay(1), ym.atEndOfMonth());
    }

    /**
     * An arbitrary date-range period. This is the extension point future non-monthly
     * frequencies (weekly, fortnightly) will use — the month/year on the resulting
     * {@link PayrollPeriod} are derived from {@code startDate} for reference/display only
     * and are not assumed to mean "this is a calendar month" anywhere else in the engine.
     */
    public PayrollPeriod of(LocalDate startDate, LocalDate endDate) {
        return new PayrollPeriod(startDate.getMonthValue(), startDate.getYear(), startDate, endDate);
    }
}