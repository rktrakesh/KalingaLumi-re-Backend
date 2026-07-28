package com.business.erp.payroll.engine.period;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayrollPeriodTest {

    private final PayrollPeriodFactory factory = new PayrollPeriodFactory(() -> LocalDate.of(2026, 6, 15));

    @Nested
    class MonthLengths {

        @ParameterizedTest(name = "{0}-{1} has {2} days")
        @CsvSource({
                "2026,1,31",  // January - 31 days
                "2026,2,28",  // February - 28 days (2026 is not a leap year)
                "2024,2,29",  // February - 29 days (2024 IS a leap year)
                "2026,4,30",  // April - 30 days
                "2026,12,31", // December - 31 days
        })
        void length_matchesCalendarMonth(int year, int month, int expectedLength) {
            PayrollPeriod period = factory.of(month, year);
            assertThat(period.length()).isEqualTo(expectedLength);
            assertThat(period.getPeriodStart()).isEqualTo(LocalDate.of(year, month, 1));
            assertThat(period.getPeriodEnd()).isEqualTo(LocalDate.of(year, month, expectedLength));
        }
    }

    @Nested
    class BoundaryComparisons {

        private final PayrollPeriod june2026 = new PayrollPeriodFactory(() -> LocalDate.of(2026, 1, 1)).of(6, 2026);

        @Test
        void hasEnded_isFalse_onTheLastDayOfThePeriod() {
            // Generation must be blocked ON the end date itself, not just before it.
            assertThat(june2026.hasEnded(LocalDate.of(2026, 6, 30))).isFalse();
        }

        @Test
        void hasEnded_isTrue_theDayAfterThePeriodEnds() {
            assertThat(june2026.hasEnded(LocalDate.of(2026, 7, 1))).isTrue();
        }

        @Test
        void hasEnded_isFalse_beforeThePeriodEnds() {
            assertThat(june2026.hasEnded(LocalDate.of(2026, 6, 15))).isFalse();
        }

        @Test
        void hasStarted_isTrue_onTheFirstDay() {
            assertThat(june2026.hasStarted(LocalDate.of(2026, 6, 1))).isTrue();
        }

        @Test
        void hasStarted_isFalse_theDayBeforeThePeriodStarts() {
            assertThat(june2026.hasStarted(LocalDate.of(2026, 5, 31))).isFalse();
        }

        @Test
        void contains_isInclusiveOfBothEndpoints() {
            assertThat(june2026.contains(LocalDate.of(2026, 6, 1))).isTrue();
            assertThat(june2026.contains(LocalDate.of(2026, 6, 30))).isTrue();
            assertThat(june2026.contains(LocalDate.of(2026, 5, 31))).isFalse();
            assertThat(june2026.contains(LocalDate.of(2026, 7, 1))).isFalse();
        }

        @Test
        void remainingDays_isZero_afterThePeriodEnds() {
            assertThat(june2026.remainingDays(LocalDate.of(2026, 7, 1))).isZero();
        }

        @Test
        void remainingDays_isFullLength_beforeThePeriodStarts() {
            assertThat(june2026.remainingDays(LocalDate.of(2026, 5, 1))).isEqualTo(30);
        }

        @Test
        void remainingDays_onTheLastDay_isZero() {
            assertThat(june2026.remainingDays(LocalDate.of(2026, 6, 30))).isZero();
        }

        @Test
        void isBefore_isTrue_whenTheWholePeriodEndedBeforeTheDate() {
            assertThat(june2026.isBefore(LocalDate.of(2026, 7, 1))).isTrue();
            assertThat(june2026.isBefore(LocalDate.of(2026, 6, 15))).isFalse();
        }

        @Test
        void isAfter_isTrue_whenTheWholePeriodStartsAfterTheDate() {
            assertThat(june2026.isAfter(LocalDate.of(2026, 5, 31))).isTrue();
            assertThat(june2026.isAfter(LocalDate.of(2026, 6, 1))).isFalse();
        }
    }

    @Nested
    class Navigation {

        @Test
        void nextPeriod_rollsOverToJanuary_fromDecember() {
            PayrollPeriod dec2026 = factory.of(12, 2026);
            PayrollPeriod next = dec2026.nextPeriod();
            assertThat(next.getYear()).isEqualTo(2027);
            assertThat(next.getMonth()).isEqualTo(1);
            assertThat(next.getPeriodStart()).isEqualTo(LocalDate.of(2027, 1, 1));
        }

        @Test
        void previousPeriod_rollsBackToDecemberOfPriorYear_fromJanuary() {
            PayrollPeriod jan2027 = factory.of(1, 2027);
            PayrollPeriod prev = jan2027.previousPeriod();
            assertThat(prev.getYear()).isEqualTo(2026);
            assertThat(prev.getMonth()).isEqualTo(12);
        }

        @Test
        void nextPeriod_handlesFebruaryLeapYearBoundary() {
            PayrollPeriod jan2024 = factory.of(1, 2024);
            PayrollPeriod feb2024 = jan2024.nextPeriod();
            assertThat(feb2024.length()).isEqualTo(29); // 2024 is a leap year
        }
    }

    @Test
    void constructor_rejectsEndDateBeforeStartDate() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.of(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)));
    }

    @Test
    void factory_current_usesInjectedClockProvider_notSystemClock() {
        PayrollPeriod current = factory.current(); // factory is fixed to 2026-06-15
        assertThat(current.getYear()).isEqualTo(2026);
        assertThat(current.getMonth()).isEqualTo(6);
    }
}