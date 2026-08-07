package com.business.erp.report.service.impl;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.expense.entity.Expense;
import com.business.erp.expense.repository.ExpenseRepository;
import com.business.erp.payroll.repository.PayrollDetailRepository;
import com.business.erp.payroll.repository.PayrollRunRepository;
import com.business.erp.report.dto.AttendanceReportResponse;
import com.business.erp.report.dto.PayrollReportResponse;
import com.business.erp.report.dto.ProfitLossResponse;
import com.business.erp.report.service.ReportService;
import com.business.erp.sales.repository.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final ExpenseRepository expenseRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    public AttendanceReportResponse attendanceReport(int year, int month) {
        log.debug("ReportServiceImpl:attendanceReport :: invoked");
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        var employees = employeeService.getAttendanceEligibleEmployees();
        var summaries = employees.stream().map(emp -> {
            var records = attendanceRepository
                    .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(emp.getId(), from, to);

            long present = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT).count();
            long absent = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT).count();
            long paidLeave = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.PAID_LEAVE).count();
            long holiday = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.HOLIDAY).count();
            int workedMins = records.stream().mapToInt(AttendanceRecord::getWorkedMinutes).sum();

            return AttendanceReportResponse.EmployeeAttendanceSummary.builder()
                    .employeeId(emp.getId()).employeeCode(emp.getEmployeeCode()).employeeName(emp.getName())
                    .presentDays((int) present).absentDays((int) absent)
                    .paidLeaveDays((int) paidLeave).holidayDays((int) holiday)
                    .totalWorkedMinutes(workedMins).build();
        }).collect(Collectors.toList());

        return AttendanceReportResponse.builder()
                .year(year).month(month).totalEmployees(employees.size()).employees(summaries).build();
    }

    public PayrollReportResponse payrollReport(int year, int month) {
        log.debug("ReportServiceImpl:payrollReport :: invoked");
        var run = payrollRunRepository.findByYearAndMonthAndIsCurrentVersionTrue(year, month)
                .orElseThrow(() -> new BusinessException("Payroll not generated for " + year + "-" + month));
        var details = payrollDetailRepository.findByPayrollRunId(run.getId());

        var summaries = details.stream().map(d -> PayrollReportResponse.PayrollDetailSummary.builder()
                .employeeCode(d.getEmployeeCode()).employeeName(d.getEmployeeName())
                .baseSalary(d.getBaseSalary()).grossSalary(d.getGrossSalary())
                .totalDeductions(d.getTotalDeductions()).netSalary(d.getNetSalary())
                .salaryCapped(d.getSalaryCapped()).build()).collect(Collectors.toList());

        return PayrollReportResponse.builder()
                .year(year).month(month).runReference(run.getRunReference())
                .totalEmployees(run.getTotalEmployees()).totalGross(run.getTotalGross())
                .totalNet(run.getTotalNet())
                .totalDeductions(details.stream().map(d -> d.getTotalDeductions())
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .details(summaries).build();
    }

    public ProfitLossResponse profitLossReport(int year, int month) {
        log.debug("ReportServiceImpl:profitLossReport :: invoked");
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        BigDecimal revenue = salesInvoiceRepository.sumRevenueByPeriod(from, to);

        // Expenses by category
        Map<String, BigDecimal> expByCategory = new LinkedHashMap<>();
        for (Expense.ExpenseCategory cat : Expense.ExpenseCategory.values()) {
            BigDecimal catTotal = expenseRepository.sumApproved(from, to, cat);
            if (catTotal.compareTo(BigDecimal.ZERO) > 0) expByCategory.put(cat.name(), catTotal);
        }
        BigDecimal totalExpenses = expByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProfitLossResponse.builder()
                .year(year).month(month)
                .totalRevenue(revenue).totalExpenses(totalExpenses)
                .expenseByCategory(expByCategory)
                .grossProfit(revenue.subtract(totalExpenses))
                .netProfit(revenue.subtract(totalExpenses))
                .build();
    }

    public ProfitLossResponse yearlyPLReport(int year) {
        log.debug("ReportServiceImpl:yearlyPLReport :: invoked");
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        BigDecimal revenue = salesInvoiceRepository.sumRevenueByPeriod(from, to);

        Map<String, BigDecimal> expByCategory = new LinkedHashMap<>();
        for (Expense.ExpenseCategory cat : Expense.ExpenseCategory.values()) {
            BigDecimal catTotal = expenseRepository.sumApproved(from, to, cat);
            if (catTotal.compareTo(BigDecimal.ZERO) > 0) expByCategory.put(cat.name(), catTotal);
        }
        BigDecimal totalExpenses = expByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProfitLossResponse.builder()
                .year(year).month(0)
                .totalRevenue(revenue).totalExpenses(totalExpenses)
                .expenseByCategory(expByCategory)
                .grossProfit(revenue.subtract(totalExpenses))
                .netProfit(revenue.subtract(totalExpenses))
                .build();
    }
}