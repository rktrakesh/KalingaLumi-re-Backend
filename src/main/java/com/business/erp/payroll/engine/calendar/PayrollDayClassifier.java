package com.business.erp.payroll.engine.calendar;

import com.business.erp.payroll.engine.leave.LeaveResolutionContext;
import com.business.erp.payroll.engine.leave.LeaveResolutionStrategy;
import com.business.erp.payroll.enums.DayStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PayrollDayClassifier {

    private final List<DayClassificationRule> rules;
    private final List<LeaveResolutionStrategy> leaveStrategies;

    public PayrollDay classify(DayClassificationInput input, LeaveResolutionContext leaveContext, int approvedOtMinutesForDate) {
        for (DayClassificationRule rule : rules) {
            Optional<PayrollDay> claimed = rule.classify(input);
            if (claimed.isPresent()) return claimed.get();
        }

        for (LeaveResolutionStrategy strategy : leaveStrategies) {
            Optional<DayStatus> claimed = strategy.resolve(input.getDate(), leaveContext);
            if (claimed.isPresent()) {
                return PayrollDay.builder()
                        .date(input.getDate()).dayStatus(claimed.get())
                        .workedMinutes(0).approvedOtMinutes(0)
                        .paidLeave(true).holidayOtEligible(false).weeklyOffOtEligible(false)
                        .attendanceExists(input.isAttendanceExists())
                        .build();
            }
        }

        boolean worked = input.isAttendancePresent() && input.getWorkedMinutes() > 0;
        if (worked) {
            return PayrollDay.builder()
                    .date(input.getDate()).dayStatus(DayStatus.PRESENT)
                    .workedMinutes(input.getWorkedMinutes()).approvedOtMinutes(approvedOtMinutesForDate)
                    .holidayOtEligible(false).weeklyOffOtEligible(false)
                    .attendanceExists(input.isAttendanceExists())
                    .build();
        }
        return PayrollDay.builder()
                .date(input.getDate()).dayStatus(DayStatus.ABSENT)
                .workedMinutes(0).approvedOtMinutes(0)
                .lossOfPay(true).holidayOtEligible(false).weeklyOffOtEligible(false)
                .attendanceExists(input.isAttendanceExists())
                .build();
    }
}