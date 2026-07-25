package com.business.erp.payroll.engine.calendar;

import com.business.erp.holiday.entity.Holiday;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class DayClassificationInput {
    LocalDate date;
    Holiday holiday;
    boolean weeklyOffDayOfWeek;
    int workedMinutes;
    boolean attendanceExists;
    boolean attendancePresent;
}