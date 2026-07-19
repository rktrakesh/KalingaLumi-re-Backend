package com.business.erp.report.service;

import com.business.erp.report.dto.AttendanceReportResponse;
import com.business.erp.report.dto.PayrollReportResponse;
import com.business.erp.report.dto.ProfitLossResponse;

public interface ReportService {
    AttendanceReportResponse attendanceReport(int year, int month);

    PayrollReportResponse payrollReport(int year, int month);

    ProfitLossResponse profitLossReport(int year, int month);

    ProfitLossResponse yearlyPLReport(int year);
}