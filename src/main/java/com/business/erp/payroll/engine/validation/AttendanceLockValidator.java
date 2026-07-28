package com.business.erp.payroll.engine.validation;

import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.common.exception.PayrollGenerationNotAllowedException;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import com.business.erp.payroll.engine.period.PayrollPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Defensive check: blocks generation if any attendance record within the period is
 * already locked by a payroll run. For today's monthly-only periods this should never
 * actually trigger — {@link DuplicatePayrollValidator} already prevents regenerating the
 * same period, which is the only way its own attendance could be locked. Its real purpose
 * is future frequencies (weekly/fortnightly) where periods can overlap: it stops a weekly
 * period from being generated over attendance days a monthly run has already locked, and
 * vice versa, without either frequency needing to know the other exists.
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class AttendanceLockValidator implements PayrollGenerationValidator {

    private final AttendanceRepository attendanceRepository;

    @Override
    public void validate(PayrollGenerationContext context) {
        PayrollPeriod period = context.getPeriod();
        if (attendanceRepository.existsByAttendanceDateBetweenAndLockedForPayrollTrue(period.getPeriodStart(), period.getPeriodEnd())) {
            throw new PayrollGenerationNotAllowedException(
                    "Attendance for part of " + period + " is already locked by another payroll run. " +
                            "Resolve the overlapping run before generating this period.");
        }
    }
}