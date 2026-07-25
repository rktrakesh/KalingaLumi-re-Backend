package com.business.erp.payroll.engine.leave;

import com.business.erp.payroll.enums.DayStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Order(1)
public class ManualLeaveAllocationStrategy implements LeaveResolutionStrategy {

    @Override
    public Optional<DayStatus> resolve(LocalDate date, LeaveResolutionContext context) {
        if (context.getApprovedLeaveDates().contains(date)) {
            return Optional.of(DayStatus.APPROVED_PAID_LEAVE);
        }
        return Optional.empty();
    }
}