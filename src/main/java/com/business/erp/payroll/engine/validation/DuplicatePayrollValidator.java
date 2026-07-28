package com.business.erp.payroll.engine.validation;

import com.business.erp.common.exception.PayrollGenerationNotAllowedException;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import com.business.erp.payroll.engine.period.PayrollPeriod;
import com.business.erp.payroll.repository.PayrollRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Blocks generation if a current version already exists for the period. Regenerating an
 * existing period is what {@code recalculate}/{@code reopen} are for — {@code generate}
 * is first-time-only.
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class DuplicatePayrollValidator implements PayrollGenerationValidator {

    private final PayrollRunRepository payrollRunRepository;

    @Override
    public void validate(PayrollGenerationContext context) {
        PayrollPeriod period = context.getPeriod();
        if (payrollRunRepository.existsByYearAndMonthAndIsCurrentVersionTrue(period.getYear(), period.getMonth())) {
            throw new PayrollGenerationNotAllowedException(
                    "Payroll for " + period.getYear() + "-" + period.getMonth() +
                            " already exists. Use recalculate or reopen instead of generating twice.");
        }
    }
}