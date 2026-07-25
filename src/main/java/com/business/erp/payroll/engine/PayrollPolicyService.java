package com.business.erp.payroll.engine;

import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import com.business.erp.settings.enums.LeaveAllocationMethod;
import com.business.erp.settings.enums.UnusedLeavePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component
public class PayrollPolicyService {

    public boolean isWeeklyOff(LocalDate date, PayrollSettingsSnapshot snapshot) {
        return parseWeeklyOffDays(snapshot).contains(date.getDayOfWeek());
    }

    public Set<DayOfWeek> parseWeeklyOffDays(PayrollSettingsSnapshot snapshot) {
        Set<DayOfWeek> days = new HashSet<>();
        String csv = snapshot.getWeeklyOffDays();
        if (csv == null || csv.isBlank()) return days;
        for (String part : csv.split(",")) {
            try {
                days.add(DayOfWeek.of(Integer.parseInt(part.trim())));
            } catch (NumberFormatException ignored) {
                // Invalid tokens are silently skipped — the snapshot was captured from validated settings.
            }
        }
        return days;
    }

    public BigDecimal overtimeMultiplier(PayrollSettingsSnapshot snapshot) {
        return snapshot.getOvertimeMultiplier();
    }

    public BigDecimal weeklyOffMultiplier(PayrollSettingsSnapshot snapshot) {
        return snapshot.getWeeklyOffMultiplier();
    }

    public BigDecimal holidayOtMultiplier(PayrollSettingsSnapshot snapshot) {
        return snapshot.getHolidayOtMultiplier();
    }

    public LeaveAllocationMethod leaveAllocationMethod(PayrollSettingsSnapshot snapshot) {
        return LeaveAllocationMethod.valueOf(snapshot.getLeaveAllocationMethod());
    }

    public UnusedLeavePolicy unusedLeavePolicy(PayrollSettingsSnapshot snapshot) {
        return UnusedLeavePolicy.valueOf(snapshot.getUnusedLeavePolicy());
    }

    public int leaveCarryForwardLimit(PayrollSettingsSnapshot snapshot) {
        return snapshot.getLeaveCarryForwardLimit();
    }

    public boolean isLeaveEncashmentEnabled(PayrollSettingsSnapshot snapshot) {
        return Boolean.TRUE.equals(snapshot.getLeaveEncashmentEnabled());
    }

    public int standardWorkingDays(PayrollSettingsSnapshot snapshot) {
        return snapshot.getStandardWorkingDays();
    }

    public int workingHoursPerDay(PayrollSettingsSnapshot snapshot) {
        return snapshot.getWorkingHoursPerDay();
    }

    public int paidLeavesPerMonth(PayrollSettingsSnapshot snapshot) {
        return snapshot.getPaidLeavesPerMonth();
    }

    /** Hourly Rate = Monthly Salary / (Standard Working Days x Working Hours Per Day). */
    public BigDecimal computeHourlyRate(BigDecimal monthlySalary, PayrollSettingsSnapshot snapshot) {
        int days = standardWorkingDays(snapshot), hours = workingHoursPerDay(snapshot);
        if (days <= 0 || hours <= 0 || monthlySalary == null) return BigDecimal.ZERO;
        return monthlySalary.divide(BigDecimal.valueOf((long) days * hours), 4, RoundingMode.HALF_UP);
    }

    /** Daily Salary = Monthly Salary / Standard Working Days — used for leave encashment. */
    public BigDecimal computeDailySalary(BigDecimal monthlySalary, PayrollSettingsSnapshot snapshot) {
        int days = standardWorkingDays(snapshot);
        if (days <= 0 || monthlySalary == null) return BigDecimal.ZERO;
        return monthlySalary.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }
}