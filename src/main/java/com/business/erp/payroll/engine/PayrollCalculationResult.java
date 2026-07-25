package com.business.erp.payroll.engine;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class PayrollCalculationResult {
    String engineVersion;
    PayrollAmounts amounts;
    int workedDays;
    int weeklyOffCount;
    int weeklyOffWorkedCount;
    int holidayCount;
    int holidayWorkedCount;
    int paidLeaveCount;
    int automaticPaidLeaveCount;
    int absentCount;

    int otMinutes;
    int holidayOtMinutes;
    int weeklyOffOtMinutes;

    BigDecimal leaveEncashmentDays;
    List<String> breakdown;
}