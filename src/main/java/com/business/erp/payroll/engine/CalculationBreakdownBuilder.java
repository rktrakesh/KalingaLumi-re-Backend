package com.business.erp.payroll.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class CalculationBreakdownBuilder {

    public List<String> build(BigDecimal basicSalary, BigDecimal hourlyRate,
                              int otMinutes, BigDecimal otMultiplier, BigDecimal overtimeAmount,
                              int weeklyOffWorkedCount, int weeklyOffOtMinutes, BigDecimal weeklyOffMultiplier, BigDecimal weeklyOffAmount,
                              int holidayWorkedCount, int holidayOtMinutes, BigDecimal holidayOtMultiplier, BigDecimal holidayOtAmount,
                              BigDecimal leaveEncashmentDays, BigDecimal dailySalary, BigDecimal leaveEncashmentAmount,
                              int absentDays, BigDecimal lossOfPayAmount,
                              BigDecimal netSalary) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("Basic Salary: Rs.%s", money(basicSalary)));

        if (otMinutes > 0) {
            lines.add(String.format("Overtime: %s Hours x Rs.%s x %sx = Rs.%s",
                    hours(otMinutes), money(hourlyRate), otMultiplier.stripTrailingZeros().toPlainString(), money(overtimeAmount)));
        }
        if (weeklyOffWorkedCount > 0) {
            lines.add(String.format("Weekly Off: %d day(s), %s Hours worked x Rs.%s x %sx = Rs.%s",
                    weeklyOffWorkedCount, hours(weeklyOffOtMinutes), money(hourlyRate),
                    weeklyOffMultiplier.stripTrailingZeros().toPlainString(), money(weeklyOffAmount)));
        }
        if (holidayWorkedCount > 0) {
            lines.add(String.format("Holiday OT: %d day(s), %s Hours worked x Rs.%s x %sx = Rs.%s",
                    holidayWorkedCount, hours(holidayOtMinutes), money(hourlyRate),
                    holidayOtMultiplier.stripTrailingZeros().toPlainString(), money(holidayOtAmount)));
        }
        if (leaveEncashmentDays != null && leaveEncashmentDays.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(String.format("Leave Encashment: %s Day(s) x Rs.%s = Rs.%s",
                    leaveEncashmentDays.stripTrailingZeros().toPlainString(), money(dailySalary), money(leaveEncashmentAmount)));
        }
        if (absentDays > 0) {
            lines.add(String.format("Loss of Pay: %d Day(s) x Rs.%s = Rs.%s",
                    absentDays, money(dailySalary), money(lossOfPayAmount)));
        }
        lines.add(String.format("Net Salary: Rs.%s", money(netSalary)));
        return lines;
    }

    private String money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String hours(int minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}