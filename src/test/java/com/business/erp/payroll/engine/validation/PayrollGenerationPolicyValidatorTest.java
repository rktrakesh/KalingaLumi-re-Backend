package com.business.erp.payroll.engine.validation;

import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.PayrollGenerationNotAllowedException;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import com.business.erp.payroll.engine.period.PayrollPeriod;
import com.business.erp.payroll.engine.period.PayrollPeriodFactory;
import com.business.erp.settings.enums.PayrollGenerationPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollGenerationPolicyValidatorTest {

    // Helper method to safely mock the updated ClockProvider
    private ClockProvider createFixedClock(LocalDate date) {
        ClockProvider clock = mock(ClockProvider.class);
        when(clock.today()).thenReturn(date);
        return clock;
    }

    private PayrollGenerationPolicyValidator validatorAsOf(LocalDate today) {
        // Replaced the lambda with our helper method
        return new PayrollGenerationPolicyValidator(createFixedClock(today));
    }

    private PayrollGenerationContext contextFor(PayrollPeriod period, PayrollGenerationPolicy policy) {
        return PayrollGenerationContext.builder()
                .period(period)
                .generationPolicy(policy)
                .requestedBy("test-user")
                .generationDate(LocalDateTime.now())
                .build();
    }

    @Test
    void blocks_whenGeneratingBeforePeriodEnds() {
        // Replaced the lambda with our helper method
        PayrollPeriod june = new PayrollPeriodFactory(createFixedClock(LocalDate.of(2026, 1, 1))).of(6, 2026); // 1 Jun - 30 Jun
        PayrollGenerationPolicyValidator validator = validatorAsOf(LocalDate.of(2026, 6, 15)); // mid-period

        assertThatThrownBy(() -> validator.validate(contextFor(june, PayrollGenerationPolicy.GENERATE_AFTER_PERIOD_END)))
                .isInstanceOf(PayrollGenerationNotAllowedException.class)
                .hasMessage("Payroll cannot be generated before the payroll period ends.");
    }

    @Test
    void blocks_whenGeneratingOnTheLastDayOfThePeriod() {
        // The period has not "ended" until the day AFTER its last day.
        PayrollPeriod june = new PayrollPeriodFactory(createFixedClock(LocalDate.of(2026, 1, 1))).of(6, 2026);
        PayrollGenerationPolicyValidator validator = validatorAsOf(LocalDate.of(2026, 6, 30)); // exactly the end date

        assertThatThrownBy(() -> validator.validate(contextFor(june, PayrollGenerationPolicy.GENERATE_AFTER_PERIOD_END)))
                .isInstanceOf(PayrollGenerationNotAllowedException.class);
    }

    @Test
    void allows_whenGeneratingTheDayAfterThePeriodEnds() {
        PayrollPeriod june = new PayrollPeriodFactory(createFixedClock(LocalDate.of(2026, 1, 1))).of(6, 2026);
        PayrollGenerationPolicyValidator validator = validatorAsOf(LocalDate.of(2026, 7, 1)); // one day after

        assertThatCode(() -> validator.validate(contextFor(june, PayrollGenerationPolicy.GENERATE_AFTER_PERIOD_END)))
                .doesNotThrowAnyException();
    }

    @Test
    void allows_wellAfterThePeriodEnds() {
        PayrollPeriod june = new PayrollPeriodFactory(createFixedClock(LocalDate.of(2026, 1, 1))).of(6, 2026);
        PayrollGenerationPolicyValidator validator = validatorAsOf(LocalDate.of(2026, 8, 15));

        assertThatCode(() -> validator.validate(contextFor(june, PayrollGenerationPolicy.GENERATE_AFTER_PERIOD_END)))
                .doesNotThrowAnyException();
    }

    @Test
    void allowDraftGeneration_neverBlocks_evenBeforeThePeriodStarts() {
        PayrollPeriod december = new PayrollPeriodFactory(createFixedClock(LocalDate.of(2026, 1, 1))).of(12, 2026);
        PayrollGenerationPolicyValidator validator = validatorAsOf(LocalDate.of(2026, 1, 1)); // long before

        assertThatCode(() -> validator.validate(contextFor(december, PayrollGenerationPolicy.ALLOW_DRAFT_GENERATION)))
                .doesNotThrowAnyException();
    }
}