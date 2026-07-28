package com.business.erp.payroll.engine.calendar;

import com.business.erp.payroll.enums.DayStatus;

import java.time.LocalDate;
import java.util.List;

public record PayrollCalendar(Long employeeId, LocalDate periodStart, LocalDate periodEnd, List<PayrollDay> days) {
    public PayrollCalendar(Long employeeId, LocalDate periodStart, LocalDate periodEnd, List<PayrollDay> days) {
        this.employeeId = employeeId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.days = List.copyOf(days);
    }

    public long countByStatus(DayStatus status) {
        return days.stream().filter(d -> d.getDayStatus() == status).count();
    }

    public long presentDays() {
        return countByStatus(DayStatus.PRESENT);
    }

    public long weeklyOffDays() {
        return countByStatus(DayStatus.WEEKLY_OFF);
    }

    public long weeklyOffWorkedDays() {
        return days.stream().filter(PayrollDay::isWeeklyOffOtEligible).count();
    }

    public int weeklyOffWorkedMinutes() {
        return days.stream().filter(PayrollDay::isWeeklyOffOtEligible).mapToInt(PayrollDay::getWorkedMinutes).sum();
    }

    public long holidayDays() {
        return countByStatus(DayStatus.FACTORY_HOLIDAY) + countByStatus(DayStatus.NATIONAL_HOLIDAY) + countByStatus(DayStatus.STATE_HOLIDAY);
    }

    public long holidayWorkedDays() {
        return days.stream().filter(PayrollDay::isHolidayOtEligible).count();
    }

    public int holidayWorkedMinutes() {
        return days.stream().filter(PayrollDay::isHolidayOtEligible).mapToInt(PayrollDay::getWorkedMinutes).sum();
    }

    public long approvedPaidLeaveDays() {
        return countByStatus(DayStatus.APPROVED_PAID_LEAVE);
    }

    public long automaticPaidLeaveDays() {
        return countByStatus(DayStatus.AUTOMATIC_PAID_LEAVE);
    }

    public long absentDays() {
        return countByStatus(DayStatus.ABSENT);
    }

    public int approvedOtMinutes() {
        return days.stream().filter(d -> d.getDayStatus() == DayStatus.PRESENT).mapToInt(PayrollDay::getApprovedOtMinutes).sum();
    }
}