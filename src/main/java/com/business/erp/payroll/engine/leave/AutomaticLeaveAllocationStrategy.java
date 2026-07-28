package com.business.erp.payroll.engine.leave;

import com.business.erp.leave.service.LeaveSettlementService;
import com.business.erp.payroll.enums.DayStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Order(2)
@RequiredArgsConstructor
public class AutomaticLeaveAllocationStrategy implements LeaveResolutionStrategy {

    private final LeaveSettlementService leaveSettlementService;

    @Override
    public Optional<DayStatus> resolve(LocalDate date, LeaveResolutionContext context) {
        if (context.isWorkedToday()) return Optional.empty();
        int consumed = leaveSettlementService.consumeAutomaticLeave(
                context.getEmployeeId(), context.getYear(), context.getMonth(), 1);
        return consumed > 0 ? Optional.of(DayStatus.AUTOMATIC_PAID_LEAVE) : Optional.empty();
    }
}