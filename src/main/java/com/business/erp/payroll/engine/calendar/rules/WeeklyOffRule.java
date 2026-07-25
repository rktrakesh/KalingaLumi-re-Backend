package com.business.erp.payroll.engine.calendar.rules;

import com.business.erp.payroll.engine.calendar.DayClassificationInput;
import com.business.erp.payroll.engine.calendar.DayClassificationRule;
import com.business.erp.payroll.engine.calendar.PayrollDay;
import com.business.erp.payroll.enums.DayStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(4)
public class WeeklyOffRule implements DayClassificationRule {

    @Override
    public Optional<PayrollDay> classify(DayClassificationInput input) {
        if (!input.isWeeklyOffDayOfWeek()) return Optional.empty();

        boolean worked = input.getWorkedMinutes() > 0;
        return Optional.of(PayrollDay.builder()
                .date(input.getDate()).dayStatus(DayStatus.WEEKLY_OFF)
                .workedMinutes(worked ? input.getWorkedMinutes() : 0)
                .approvedOtMinutes(0)
                .weeklyOff(true)
                .holidayOtEligible(false)
                .weeklyOffOtEligible(worked)
                .attendanceExists(input.isAttendanceExists())
                .build());
    }
}