package com.business.erp.payroll.engine.leave;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Set;

@Value
@Builder
public class LeaveResolutionContext {
    Long employeeId;
    int year;
    int month;
    /** Dates within the period with an employee-submitted, manager-approved leave request. */
    Set<LocalDate> approvedLeaveDates;
    /** True if attendance shows the employee actually worked this date. */
    boolean workedToday;
}