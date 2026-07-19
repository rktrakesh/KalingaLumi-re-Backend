package com.business.erp.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttendanceReportResponse {
    private int year;
    private int month;
    private int totalEmployees;
    private List<EmployeeAttendanceSummary> employees;

    @Data
    @Builder
    public static class EmployeeAttendanceSummary {
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private int presentDays;
        private int absentDays;
        private int paidLeaveDays;
        private int holidayDays;
        private int totalWorkedMinutes;
    }
}
