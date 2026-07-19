package com.business.erp.payroll.service.impl;

import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.repository.EmployeeSalaryHistoryRepository;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.leave.repository.LeaveBalanceRepository;
import com.business.erp.loan.service.LoanService;
import com.business.erp.monthclosing.service.MonthClosingService;
import com.business.erp.overtime.service.OvertimeService;
import com.business.erp.payroll.dto.request.GeneratePayrollRequest;
import com.business.erp.payroll.dto.response.PayrollDetailResponse;
import com.business.erp.payroll.dto.response.PayrollRunResponse;
import com.business.erp.payroll.entity.PayrollDetail;
import com.business.erp.payroll.entity.PayrollRun;
import com.business.erp.payroll.repository.PayrollDetailRepository;
import com.business.erp.payroll.repository.PayrollRunRepository;
import com.business.erp.payroll.service.PayrollService;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final EmployeeService employeeService;
    private final EmployeeSalaryHistoryRepository salaryHistoryRepository;
    private final AttendanceRepository attendanceRepository;
    private final OvertimeService overtimeService;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LoanService loanService;
    private final SettingsService settingsService;
    private final ReferenceNumberService refService;
    private final MonthClosingService monthClosingService;
    private final CashbookService cashbookService;
    private final AuditService auditService;
    private final Logger log = LoggerFactory.getLogger(PayrollServiceImpl.class);

    @Transactional
    public PayrollRunResponse generate(GeneratePayrollRequest req, String generatedBy) {
        log.info("PayrollServiceImpl:method :: invoked");
        log.debug("PayrollServiceImpl:generate :: invoked");
        int year = req.getYear(), month = req.getMonth();
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        if (LocalDate.now().isBefore(periodEnd))
            throw new BusinessException("Cannot generate payroll before month ends on " + periodEnd);
        if (monthClosingService.isMonthClosed(year, month))
            throw new BusinessException("Month is closed. Use regenerate.");

        // Delete existing run if any (regeneration path)
        payrollRunRepository.findByYearAndMonth(year, month).ifPresent(existing -> {
            payrollDetailRepository.deleteByPayrollRunId(existing.getId());
            payrollRunRepository.delete(existing);
        });

        // Resolve settings as of period start date
        int stdWorkDays = settingsService.getIntValueAsOf(SettingKey.STANDARD_WORKING_DAYS, periodStart);
        int stdWorkHours = settingsService.getIntValueAsOf(SettingKey.STANDARD_WORKING_HOURS, periodStart);
        BigDecimal otMultiplier = settingsService.getDecimalValueAsOf(SettingKey.OVERTIME_MULTIPLIER, periodStart);

        String ref = refService.generatePayrollReference(periodStart);
        PayrollRun run = payrollRunRepository.save(PayrollRun.builder()
                .runReference(ref).year(year).month(month)
                .periodStart(periodStart).periodEnd(periodEnd)
                .status(PayrollRun.PayrollStatus.GENERATED)
                .generatedBy(generatedBy).generatedDate(LocalDateTime.now())
                .remarks(req.getRemarks()).build());

        List<Employee> activeEmps = employeeService.getActiveEmployees();
        BigDecimal totalGross = BigDecimal.ZERO, totalNet = BigDecimal.ZERO;

        for (Employee emp : activeEmps) {
            PayrollDetail detail = computeDetail(emp, run, periodStart, periodEnd,
                    stdWorkDays, stdWorkHours, otMultiplier, generatedBy);
            payrollDetailRepository.save(detail);
            totalGross = totalGross.add(detail.getGrossSalary());
            totalNet = totalNet.add(detail.getNetSalary());

            // Post salary to cashbook
            cashbookService.recordSalaryPayment(detail.getNetSalary(), ref, emp.getName());
        }

        run.setTotalEmployees(activeEmps.size());
        run.setTotalGross(totalGross.setScale(2, RoundingMode.HALF_UP));
        run.setTotalNet(totalNet.setScale(2, RoundingMode.HALF_UP));
        PayrollRun saved = payrollRunRepository.save(run);

        auditService.log("PAYROLL", "GENERATE", "PayrollRun", saved.getId());
        return toRunResponse(saved);
    }

    @Transactional
    public PayrollRunResponse regenerate(Long runId, String generatedBy) {
        log.info("PayrollServiceImpl:method :: invoked");
        log.debug("PayrollServiceImpl:regenerate :: invoked");
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", runId));
        if (run.getStatus() == PayrollRun.PayrollStatus.LOCKED)
            throw new BusinessException("Payroll is locked and cannot be regenerated");

        GeneratePayrollRequest req = new GeneratePayrollRequest();
        req.setYear(run.getYear());
        req.setMonth(run.getMonth());
        req.setRemarks("Regenerated by " + generatedBy);
        PayrollRunResponse response = generate(req, generatedBy);

        // Mark status as REGENERATED
        payrollRunRepository.findById(response.getId()).ifPresent(r -> {
            r.setStatus(PayrollRun.PayrollStatus.REGENERATED);
            payrollRunRepository.save(r);
        });
        auditService.log("PAYROLL", "REGENERATE", "PayrollRun", response.getId());
        return response;
    }

    private PayrollDetail computeDetail(Employee emp, PayrollRun run,
                                        LocalDate from, LocalDate to,
                                        int stdWorkDays, int stdWorkHours,
                                        BigDecimal otMultiplier, String createdBy) {
        // Resolve salary as of period start
        BigDecimal salary = salaryHistoryRepository.findSalaryAsOf(emp.getId(), from)
                .map(h -> h.getSalary())
                .orElse(emp.getCurrentSalary());

        // hourly rate = salary / (stdWorkDays * stdWorkHours)
        BigDecimal hourlyRate = salary.divide(
                BigDecimal.valueOf((long) stdWorkDays * stdWorkHours), 4, RoundingMode.HALF_UP);

        // Worked minutes from attendance
        Integer workedMins = attendanceRepository.sumWorkedMinutesByEmployeeAndMonth(emp.getId(), from, to);
        int totalWorkedMins = workedMins != null ? workedMins : 0;

        // Paid leave days
        int paidLeaveDays = leaveBalanceRepository.findByEmployeeIdAndYearAndMonth(
                        emp.getId(), run.getYear(), run.getMonth())
                .map(b -> b.getUsed()).orElse(0);

        // Overtime minutes (approved)
        int otMins = overtimeService.getApprovedMinutesForPayroll(emp.getId(), from, to);

        // Gross salary: (workedMins / 60 * hourlyRate) + paidLeave days value + OT
        BigDecimal workedHours = BigDecimal.valueOf(totalWorkedMins).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal workedPay = hourlyRate.multiply(workedHours);

        BigDecimal leavePay = hourlyRate
                .multiply(BigDecimal.valueOf(stdWorkHours))
                .multiply(BigDecimal.valueOf(paidLeaveDays));

        BigDecimal otHours = BigDecimal.valueOf(otMins).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal otPay = hourlyRate.multiply(otMultiplier).multiply(otHours);

        BigDecimal grossSalary = workedPay.add(leavePay).add(otPay)
                .setScale(2, RoundingMode.HALF_UP);

        // Loan deductions
        BigDecimal[] loanDeductions = loanService.calculateAndPostDeductions(
                emp.getId(), from, grossSalary, createdBy);
        BigDecimal loanInterest = loanDeductions[0];
        BigDecimal loanPrincipal = loanDeductions[1];
        BigDecimal totalDeductions = loanInterest.add(loanPrincipal);

        // Net salary - never negative
        BigDecimal netSalary = grossSalary.subtract(totalDeductions).max(BigDecimal.ZERO);
        boolean capped = totalDeductions.compareTo(grossSalary) > 0;

        return PayrollDetail.builder()
                .payrollRun(run).employee(emp)
                .employeeCode(emp.getEmployeeCode()).employeeName(emp.getName())
                .baseSalary(salary).standardWorkDays(stdWorkDays).standardWorkHours(stdWorkHours)
                .hourlyRate(hourlyRate).workedMinutes(totalWorkedMins)
                .paidLeaveDays(paidLeaveDays).overtimeMinutes(otMins)
                .overtimeMultiplier(otMultiplier).grossSalary(grossSalary)
                .loanInterestDeduction(loanInterest).loanPrincipalDeduction(loanPrincipal)
                .totalDeductions(totalDeductions).netSalary(netSalary)
                .salaryCapped(capped).createdBy(createdBy).build();
    }

    public List<PayrollRunResponse> getAllRuns() {
        log.debug("PayrollServiceImpl:getAllRuns :: invoked");
        return payrollRunRepository.findAll().stream()
                .map(this::toRunResponse).collect(Collectors.toList());
    }

    public PayrollRunResponse getRun(Long runId) {
        log.debug("PayrollServiceImpl:getRun :: invoked");
        return toRunResponse(payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", runId)));
    }

    public List<PayrollDetailResponse> getDetails(Long runId) {
        log.debug("PayrollServiceImpl:getDetails :: invoked");
        return payrollDetailRepository.findByPayrollRunId(runId)
                .stream().map(this::toDetailResponse).collect(Collectors.toList());
    }

    public PayrollDetailResponse getEmployeePayslip(Long empId, int year, int month) {
        log.debug("PayrollServiceImpl:getEmployeePayslip :: invoked");
        return payrollDetailRepository.findByPayrollRun_YearAndPayrollRun_MonthAndEmployeeId(year, month, empId)
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payslip not found for employee " + empId + " for " + year + "-" + month));
    }

    private PayrollRunResponse toRunResponse(PayrollRun r) {
        return PayrollRunResponse.builder()
                .id(r.getId()).runReference(r.getRunReference())
                .year(r.getYear()).month(r.getMonth())
                .periodStart(r.getPeriodStart()).periodEnd(r.getPeriodEnd())
                .status(r.getStatus().name()).totalEmployees(r.getTotalEmployees())
                .totalGross(r.getTotalGross()).totalNet(r.getTotalNet())
                .generatedBy(r.getGeneratedBy()).generatedDate(r.getGeneratedDate())
                .remarks(r.getRemarks()).build();
    }

    private PayrollDetailResponse toDetailResponse(PayrollDetail d) {
        return PayrollDetailResponse.builder()
                .id(d.getId()).employeeId(d.getEmployee().getId())
                .employeeCode(d.getEmployeeCode()).employeeName(d.getEmployeeName())
                .baseSalary(d.getBaseSalary()).standardWorkDays(d.getStandardWorkDays())
                .standardWorkHours(d.getStandardWorkHours()).hourlyRate(d.getHourlyRate())
                .workedMinutes(d.getWorkedMinutes()).paidLeaveDays(d.getPaidLeaveDays())
                .overtimeMinutes(d.getOvertimeMinutes()).overtimeMultiplier(d.getOvertimeMultiplier())
                .grossSalary(d.getGrossSalary())
                .loanInterestDeduction(d.getLoanInterestDeduction())
                .loanPrincipalDeduction(d.getLoanPrincipalDeduction())
                .totalDeductions(d.getTotalDeductions()).netSalary(d.getNetSalary())
                .salaryCapped(d.getSalaryCapped()).paymentStatus(d.getPaymentStatus().name()).build();
    }
}
