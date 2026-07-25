package com.business.erp.payroll.engine.calendar.rules;

import com.business.erp.holiday.entity.Holiday;
import com.business.erp.payroll.enums.DayStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class NationalHolidayRule extends AbstractWorkAllowedHolidayRule {
    @Override
    protected Holiday.HolidayType matchType() {
        return Holiday.HolidayType.NATIONAL_HOLIDAY;
    }

    @Override
    protected DayStatus resultStatus() {
        return DayStatus.NATIONAL_HOLIDAY;
    }
}