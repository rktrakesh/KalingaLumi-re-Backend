package com.business.erp.payroll.engine.calendar;

import com.business.erp.holiday.entity.Holiday;
import com.business.erp.payroll.enums.DayStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class PayrollDay {
    LocalDate date;
    DayStatus dayStatus;
    int workedMinutes;
    int approvedOtMinutes;
    boolean weeklyOff;
    boolean holiday;
    Holiday.HolidayType holidayType;
    boolean paidLeave;
    boolean lossOfPay;
    boolean holidayOtEligible;
    boolean weeklyOffOtEligible;
    boolean attendanceExists;
}