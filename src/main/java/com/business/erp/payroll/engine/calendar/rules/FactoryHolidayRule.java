package com.business.erp.payroll.engine.calendar.rules;

import com.business.erp.holiday.entity.Holiday;
import com.business.erp.payroll.engine.calendar.DayClassificationInput;
import com.business.erp.payroll.engine.calendar.DayClassificationRule;
import com.business.erp.payroll.engine.calendar.PayrollDay;
import com.business.erp.payroll.enums.DayStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(1)
public class FactoryHolidayRule implements DayClassificationRule {

    @Override
    public Optional<PayrollDay> classify(DayClassificationInput input) {
        if (input.getHoliday() == null || input.getHoliday().getHolidayType() != Holiday.HolidayType.FACTORY_HOLIDAY) {
            return Optional.empty();
        }
        return Optional.of(PayrollDay.builder()
                .date(input.getDate()).dayStatus(DayStatus.FACTORY_HOLIDAY)
                .workedMinutes(0).approvedOtMinutes(0)
                .holiday(true).holidayType(Holiday.HolidayType.FACTORY_HOLIDAY)
                .holidayOtEligible(false).weeklyOffOtEligible(false)
                .attendanceExists(input.isAttendanceExists())
                .build());
    }
}