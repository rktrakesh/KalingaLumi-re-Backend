package com.business.erp.payroll.engine.leave;

import com.business.erp.payroll.enums.DayStatus;

import java.time.LocalDate;
import java.util.Optional;

public interface LeaveResolutionStrategy {

    Optional<DayStatus> resolve(LocalDate date, LeaveResolutionContext context);
}