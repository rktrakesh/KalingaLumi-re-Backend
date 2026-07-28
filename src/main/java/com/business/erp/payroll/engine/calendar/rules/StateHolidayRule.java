package com.business.erp.payroll.engine.calendar.rules;

import com.business.erp.holiday.entity.Holiday;
import com.business.erp.payroll.enums.DayStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class StateHolidayRule extends AbstractWorkAllowedHolidayRule {
    @Override
    protected Holiday.HolidayType matchType() {
        return Holiday.HolidayType.STATE_HOLIDAY;
    }

    @Override
    protected DayStatus resultStatus() {
        return DayStatus.STATE_HOLIDAY;
    }
}