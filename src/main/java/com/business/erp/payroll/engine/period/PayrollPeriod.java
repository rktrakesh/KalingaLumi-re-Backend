package com.business.erp.payroll.engine.period;

import lombok.Value;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Immutable value object representing one payroll period. This is the single source of
 * truth for every payroll-date comparison in the system — no other class should ever
 * compare {@code LocalDate}s against a payroll period's start/end directly.
 * <p>
 * Currently always a calendar month (created via {@link PayrollPeriodFactory#of(int, int)}),
 * but {@link PayrollPeriodFactory#of(LocalDate, LocalDate)} allows an arbitrary date range,
 * which is what future weekly/fortnightly payroll frequencies will use without requiring
 * any change to this class or anything that consumes it.
 * <p>
 * Always create instances via {@link PayrollPeriodFactory} — never construct one directly
 * outside this package.
 */
@Value
public class PayrollPeriod {
    int month;
    int year;
    LocalDate periodStart;
    LocalDate periodEnd;

    PayrollPeriod(int month, int year, LocalDate periodStart, LocalDate periodEnd) {
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Payroll period end (" + periodEnd + ") cannot be before start (" + periodStart + ")");
        }
        this.month = month;
        this.year = year;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    /** True once {@code date} has reached or passed the first day of this period. */
    public boolean hasStarted(LocalDate date) {
        return !date.isBefore(periodStart);
    }

    /** True once {@code date} is strictly after the last day of this period (the period is fully over). */
    public boolean hasEnded(LocalDate date) {
        return date.isAfter(periodEnd);
    }

    /** True if {@code date} falls anywhere within [periodStart, periodEnd] inclusive. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(periodStart) && !date.isAfter(periodEnd);
    }

    /** Alias for {@link #contains(LocalDate)} — reads better at call sites asking "is this the active period?". */
    public boolean isCurrentPeriod(LocalDate date) {
        return contains(date);
    }

    /**
     * Days left in the period as of {@code date}. Zero if the period has already ended;
     * the full {@link #length()} if {@code date} is before the period starts.
     */
    public long remainingDays(LocalDate date) {
        if (date.isAfter(periodEnd)) return 0;
        if (date.isBefore(periodStart)) return length();
        return ChronoUnit.DAYS.between(date, periodEnd);
    }

    /** Total number of days in this period, inclusive of both endpoints. */
    public int length() {
        return (int) ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
    }

    /** True if this entire period occurred before {@code date} (i.e. the period has ended as of that date). */
    public boolean isBefore(LocalDate date) {
        return periodEnd.isBefore(date);
    }

    /** True if this entire period occurs after {@code date} (i.e. the period hasn't started yet as of that date). */
    public boolean isAfter(LocalDate date) {
        return periodStart.isAfter(date);
    }

    /** The calendar-month period immediately following this one. */
    public PayrollPeriod nextPeriod() {
        YearMonth next = YearMonth.of(year, month).plusMonths(1);
        return new PayrollPeriod(next.getMonthValue(), next.getYear(),
                next.atDay(1), next.atEndOfMonth());
    }

    /** The calendar-month period immediately preceding this one. */
    public PayrollPeriod previousPeriod() {
        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        return new PayrollPeriod(prev.getMonthValue(), prev.getYear(),
                prev.atDay(1), prev.atEndOfMonth());
    }

    @Override
    public String toString() {
        return String.format("PayrollPeriod[%04d-%02d: %s..%s]", year, month, periodStart, periodEnd);
    }
}