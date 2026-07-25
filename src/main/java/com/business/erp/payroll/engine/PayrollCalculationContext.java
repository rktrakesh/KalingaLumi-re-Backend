package com.business.erp.payroll.engine;

import com.business.erp.employee.entity.Employee;
import com.business.erp.payroll.engine.calendar.PayrollCalendar;
import com.business.erp.payroll.entity.PayrollRun;
import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PayrollCalculationContext {
    Employee employee;
    PayrollRun payrollRun;
    PayrollSettingsSnapshot snapshot;
    PayrollCalendar calendar;
    BigDecimal salary;
    int standardWorkingDays;
    int workingHoursPerDay;
    BigDecimal hourlyRate;

    /**
     * Days/amount already decided by {@code LeaveSettlementService} for this period (EXPIRE/ENCASH/
     * CARRY_FORWARD is resolved upstream by the orchestrator before the engine runs, so the engine
     * itself performs no leave-module side effects — it only folds an already-settled amount into net pay).
     */
    @Builder.Default
    BigDecimal leaveEncashmentDays = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal leaveEncashmentAmount = BigDecimal.ZERO;
}