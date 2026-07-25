package com.business.erp.payroll.engine;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PayrollAmounts {
    BigDecimal basicSalary;
    BigDecimal hourlyRate;
    BigDecimal dailyRate;

    BigDecimal otAmount;
    BigDecimal weeklyOffAmount;
    BigDecimal holidayOtAmount;
    BigDecimal leaveEncashmentAmount;
    BigDecimal lossOfPayAmount;

    BigDecimal grossSalary;
    BigDecimal netSalary;
}