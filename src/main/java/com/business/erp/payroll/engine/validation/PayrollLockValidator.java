package com.business.erp.payroll.engine.validation;

import com.business.erp.common.exception.PayrollGenerationNotAllowedException;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import com.business.erp.payroll.engine.period.PayrollPeriod;
import com.business.erp.payroll.enums.PayrollStatus;
import com.business.erp.payroll.repository.PayrollRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Blocks generation with a specific, actionable message when the period's most recent
 * version has already reached {@link PayrollStatus#LOCKED} — a terminal state that must
 * never be silently reprocessed. Runs before {@link DuplicatePayrollValidator} so a
 * locked period gets this precise message rather than the generic "already exists" one.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class PayrollLockValidator implements PayrollGenerationValidator {

    private final PayrollRunRepository payrollRunRepository;

    @Override
    public void validate(PayrollGenerationContext context) {
        PayrollPeriod period = context.getPeriod();
        payrollRunRepository.findByYearAndMonthAndIsCurrentVersionTrue(period.getYear(), period.getMonth())
                .filter(run -> run.getStatus() == PayrollStatus.LOCKED)
                .ifPresent(run -> {
                    throw new PayrollGenerationNotAllowedException(
                            "Payroll for " + period.getYear() + "-" + period.getMonth() +
                                    " is permanently locked (run " + run.getRunReference() + ") and cannot be regenerated.");
                });
    }
}