package com.business.erp.payroll.engine.calendar.rules;

import com.business.erp.holiday.entity.Holiday;
import com.business.erp.payroll.engine.calendar.DayClassificationInput;
import com.business.erp.payroll.engine.calendar.DayClassificationRule;
import com.business.erp.payroll.engine.calendar.PayrollDay;
import com.business.erp.payroll.enums.DayStatus;

import java.util.Optional;

abstract class AbstractWorkAllowedHolidayRule implements DayClassificationRule {

    protected abstract Holiday.HolidayType matchType();

    protected abstract DayStatus resultStatus();

    @Override
    public Optional<PayrollDay> classify(DayClassificationInput input) {
        Holiday holiday = input.getHoliday();
        if (holiday == null || holiday.getHolidayType() != matchType()) return Optional.empty();

        boolean workAllowed = Boolean.TRUE.equals(holiday.getWorkAllowed());
        boolean worked = workAllowed && input.getWorkedMinutes() > 0;

        return Optional.of(PayrollDay.builder()
                .date(input.getDate()).dayStatus(resultStatus())
                .workedMinutes(worked ? input.getWorkedMinutes() : 0)
                .approvedOtMinutes(0)
                .holiday(true).holidayType(matchType())
                .holidayOtEligible(worked)
                .weeklyOffOtEligible(false)
                .attendanceExists(input.isAttendanceExists())
                .build());
    }
}