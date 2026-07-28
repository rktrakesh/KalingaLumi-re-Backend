package com.business.erp.payroll.engine.validation;

import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.PayrollGenerationNotAllowedException;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import com.business.erp.payroll.engine.period.PayrollPeriod;
import com.business.erp.settings.enums.PayrollGenerationPolicy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Enforces the {@code PAYROLL_GENERATION_POLICY} company setting:
 * <ul>
 *     <li>{@link PayrollGenerationPolicy#GENERATE_AFTER_PERIOD_END} — generation is only
 *     allowed once {@link PayrollPeriod#hasEnded(LocalDate)} is true for today.</li>
 *     <li>{@link PayrollGenerationPolicy#ALLOW_DRAFT_GENERATION} — no date restriction;
 *     existing DRAFT-starting lifecycle behaviour applies unchanged.</li>
 * </ul>
 * This is the single place payroll-generation date/policy logic lives — nothing else in
 * the codebase should compare "today" against a payroll period to decide whether
 * generation is allowed.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class PayrollGenerationPolicyValidator implements PayrollGenerationValidator {

    private final ClockProvider clockProvider;
    private final Logger log = LoggerFactory.getLogger(PayrollGenerationPolicyValidator.class);

    @Override
    public void validate(PayrollGenerationContext context) {
        PayrollGenerationPolicy policy = context.getGenerationPolicy();
        PayrollPeriod period = context.getPeriod();

        if (policy == PayrollGenerationPolicy.ALLOW_DRAFT_GENERATION) {
            log.debug("PayrollGenerationPolicyValidator:validate :: policy=ALLOW_DRAFT_GENERATION — no date restriction");
            return;
        }

        LocalDate today = clockProvider.today();
        if (!period.hasEnded(today)) {
            log.warn("PayrollGenerationPolicyValidator:validate :: BLOCKED period={} today={} — period has not ended", period, today);
            throw new PayrollGenerationNotAllowedException("Payroll cannot be generated before the payroll period ends.");
        }
        log.debug("PayrollGenerationPolicyValidator:validate :: period={} today={} — period has ended, generation allowed", period, today);
    }
}