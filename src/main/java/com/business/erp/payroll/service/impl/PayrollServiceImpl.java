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
import com.business.erp.payroll.dto.request.DisbursePaymentRequest;
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

    // ─────────────────────────────────────────────────────────────────────────
    // GENERATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollRunResponse generate(GeneratePayrollRequest req, String generatedBy) {
        log.info("PayrollServiceImpl:generate :: year={} month={} by={}", req.getYear(), req.getMonth(), generatedBy);
        int year = req.getYear(), month = req.getMonth();
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        if (LocalDate.now().isBefore(periodEnd))
            throw new BusinessException("Cannot generate payroll before month ends on " + periodEnd);
        if (monthClosingService.isMonthClosed(year, month))
            throw new BusinessException("Month is closed. Use regenerate.");

        payrollRunRepository.findByYearAndMonth(year, month).ifPresent(existing -> {
            payrollDetailRepository.deleteByPayrollRunId(existing.getId());
            payrollRunRepository.delete(existing);
        });

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
        }

        run.setTotalEmployees(activeEmps.size());
        run.setTotalGross(totalGross.setScale(2, RoundingMode.HALF_UP));
        run.setTotalNet(totalNet.setScale(2, RoundingMode.HALF_UP));
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log("PAYROLL", "GENERATE", "PayrollRun", saved.getId());
        log.info("PayrollServiceImpl:generate :: SUCCESS runId={} ref={} employees={}", saved.getId(), ref, activeEmps.size());
        return toRunResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGENERATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollRunResponse regenerate(Long runId, String generatedBy) {
        log.info("PayrollServiceImpl:regenerate :: runId={} by={}", runId, generatedBy);
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", runId));
        if (run.getStatus() == PayrollRun.PayrollStatus.LOCKED)
            throw new BusinessException("Payroll is LOCKED and cannot be regenerated. Unlock first.");

        // Check if any employees have already been paid — warn
        long paidCount = payrollDetailRepository.countByPayrollRunIdAndPaymentStatus(
                runId, PayrollDetail.PaymentStatus.PAID);
        if (paidCount > 0)
            throw new BusinessException("Cannot regenerate — " + paidCount + " employee(s) already marked PAID. Lock the run instead.");

        GeneratePayrollRequest req = new GeneratePayrollRequest();
        req.setYear(run.getYear());
        req.setMonth(run.getMonth());
        req.setRemarks("Regenerated by " + generatedBy);
        PayrollRunResponse response = generate(req, generatedBy);

        payrollRunRepository.findById(response.getId()).ifPresent(r -> {
            r.setStatus(PayrollRun.PayrollStatus.REGENERATED);
            payrollRunRepository.save(r);
        });
        auditService.log("PAYROLL", "REGENERATE", "PayrollRun", response.getId());
        log.info("PayrollServiceImpl:regenerate :: SUCCESS newRunId={}", response.getId());
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DISBURSE ONE employee salary
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollDetailResponse disburseOne(Long detailId, DisbursePaymentRequest req, String paidBy) {
        log.info("PayrollServiceImpl:disburseOne :: detailId={} mode={} by={}", detailId, req.getPaymentMode(), paidBy);
        PayrollDetail detail = payrollDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDetail", detailId));

        if (detail.getPaymentStatus() == PayrollDetail.PaymentStatus.PAID)
            throw new BusinessException(detail.getEmployeeName() + " salary is already marked as PAID");

        if (detail.getPayrollRun().getStatus() == PayrollRun.PayrollStatus.LOCKED)
            throw new BusinessException("Payroll run is LOCKED");

        // Mark as paid
        detail.setPaymentStatus(PayrollDetail.PaymentStatus.PAID);
        detail.setPaidDate(LocalDate.now());
        detail.setPaidBy(paidBy);
        detail.setPaymentMode(req.getPaymentMode().name());
        PayrollDetail saved = payrollDetailRepository.save(detail);

        // Auto-create cashbook debit entry
        String ref = detail.getPayrollRun().getRunReference();
        cashbookService.recordSalaryPayment(detail.getNetSalary(), ref, detail.getEmployeeName());

        auditService.log("PAYROLL", "DISBURSE", "PayrollDetail", detailId);
        log.info("PayrollServiceImpl:disburseOne :: SUCCESS emp={} amount={} mode={}",
                detail.getEmployeeCode(), detail.getNetSalary(), req.getPaymentMode());
        return toDetailResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DISBURSE ALL — pay all PENDING employees in one run
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void disburseAll(Long runId, DisbursePaymentRequest req, String paidBy) {
        log.info("PayrollServiceImpl:disburseAll :: runId={} mode={} by={}", runId, req.getPaymentMode(), paidBy);
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", runId));
        if (run.getStatus() == PayrollRun.PayrollStatus.LOCKED)
            throw new BusinessException("Payroll run is LOCKED");

        List<PayrollDetail> pending = payrollDetailRepository
                .findByPayrollRunIdAndPaymentStatus(runId, PayrollDetail.PaymentStatus.PENDING);

        if (pending.isEmpty())
            throw new BusinessException("All employees in this run are already PAID");

        int count = 0;
        for (PayrollDetail detail : pending) {
            detail.setPaymentStatus(PayrollDetail.PaymentStatus.PAID);
            detail.setPaidDate(LocalDate.now());
            detail.setPaidBy(paidBy);
            detail.setPaymentMode(req.getPaymentMode().name());
            payrollDetailRepository.save(detail);
            cashbookService.recordSalaryPayment(detail.getNetSalary(),
                    run.getRunReference(), detail.getEmployeeName());
            count++;
        }

        auditService.log("PAYROLL", "DISBURSE_ALL", "PayrollRun", runId);
        log.info("PayrollServiceImpl:disburseAll :: SUCCESS runId={} disbursed={} employees", runId, count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCK RUN — prevent further changes
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollRunResponse lockRun(Long runId, String lockedBy) {
        log.info("PayrollServiceImpl:lockRun :: runId={} by={}", runId, lockedBy);
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", runId));
        if (run.getStatus() == PayrollRun.PayrollStatus.LOCKED)
            throw new BusinessException("Payroll run is already LOCKED");

        long unpaid = payrollDetailRepository.countByPayrollRunIdAndPaymentStatus(
                runId, PayrollDetail.PaymentStatus.PENDING);
        if (unpaid > 0)
            throw new BusinessException("Cannot lock — " + unpaid + " employee(s) still have PENDING salary. Pay them first.");

        run.setStatus(PayrollRun.PayrollStatus.LOCKED);
        run.setLockedBy(lockedBy);
        run.setLockedDate(LocalDateTime.now());
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log("PAYROLL", "LOCK", "PayrollRun", runId);
        log.info("PayrollServiceImpl:lockRun :: SUCCESS runId={}", runId);
        return toRunResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ methods — @Transactional(readOnly=true) to fix LazyInitializationException
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getAllRuns() {
        log.debug("PayrollServiceImpl:getAllRuns :: fetching all payroll runs");
        return payrollRunRepository.findAllByOrderByYearDescMonthDesc()
                .stream().map(this::toRunResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRunResponse getRun(Long runId) {
        log.debug("PayrollServiceImpl:getRun :: runId={}", runId);
        return toRunResponse(payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", runId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollDetailResponse> getDetails(Long runId) {
        log.debug("PayrollServiceImpl:getDetails :: runId={}", runId);
        return payrollDetailRepository.findByPayrollRunId(runId)
                .stream().map(this::toDetailResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDetailResponse getEmployeePayslip(Long empId, int year, int month) {
        log.debug("PayrollServiceImpl:getEmployeePayslip :: empId={} {}/{}", empId, year, month);
        return payrollDetailRepository
                .findByPayrollRun_YearAndPayrollRun_MonthAndEmployeeId(year, month, empId)
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payslip not found for employee " + empId + " for " + year + "-" + month));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private PayrollDetail computeDetail(Employee emp, PayrollRun run,
                                        LocalDate from, LocalDate to,
                                        int stdWorkDays, int stdWorkHours,
                                        BigDecimal otMultiplier, String createdBy) {
        BigDecimal salary = salaryHistoryRepository.findSalaryAsOf(emp.getId(), from)
                .map(h -> h.getSalary())
                .orElse(emp.getCurrentSalary());

        BigDecimal hourlyRate = salary.divide(
                BigDecimal.valueOf((long) stdWorkDays * stdWorkHours), 4, RoundingMode.HALF_UP);

        Integer workedMins = attendanceRepository.sumWorkedMinutesByEmployeeAndMonth(emp.getId(), from, to);
        int totalWorked = workedMins != null ? workedMins : 0;

        int paidLeaveDays = leaveBalanceRepository
                .findByEmployeeIdAndYearAndMonth(emp.getId(), run.getYear(), run.getMonth())
                .map(b -> b.getUsed()).orElse(0);

        int otMins = overtimeService.getApprovedMinutesForPayroll(emp.getId(), from, to);

        BigDecimal workedHours = BigDecimal.valueOf(totalWorked).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal workedPay = hourlyRate.multiply(workedHours);
        BigDecimal leavePay = hourlyRate.multiply(BigDecimal.valueOf(stdWorkHours)).multiply(BigDecimal.valueOf(paidLeaveDays));
        BigDecimal otHours = BigDecimal.valueOf(otMins).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal otPay = hourlyRate.multiply(otMultiplier).multiply(otHours);
        BigDecimal grossSalary = workedPay.add(leavePay).add(otPay).setScale(2, RoundingMode.HALF_UP);

        BigDecimal[] loanDeductions = loanService.calculateAndPostDeductions(emp.getId(), from, grossSalary, createdBy);
        BigDecimal loanInterest = loanDeductions[0];
        BigDecimal loanPrincipal = loanDeductions[1];
        BigDecimal totalDeductions = loanInterest.add(loanPrincipal);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions).max(BigDecimal.ZERO);
        boolean capped = totalDeductions.compareTo(grossSalary) > 0;

        log.debug("PayrollServiceImpl:computeDetail :: emp={} gross={} deductions={} net={} capped={}",
                emp.getEmployeeCode(), grossSalary, totalDeductions, netSalary, capped);

        return PayrollDetail.builder()
                .payrollRun(run).employee(emp)
                .employeeCode(emp.getEmployeeCode()).employeeName(emp.getName())
                .baseSalary(salary).standardWorkDays(stdWorkDays).standardWorkHours(stdWorkHours)
                .hourlyRate(hourlyRate).workedMinutes(totalWorked)
                .paidLeaveDays(paidLeaveDays).overtimeMinutes(otMins)
                .overtimeMultiplier(otMultiplier).grossSalary(grossSalary)
                .loanInterestDeduction(loanInterest).loanPrincipalDeduction(loanPrincipal)
                .totalDeductions(totalDeductions).netSalary(netSalary)
                .salaryCapped(capped).createdBy(createdBy).build();
    }

    private PayrollRunResponse toRunResponse(PayrollRun r) {
        return PayrollRunResponse.builder()
                .id(r.getId()).runReference(r.getRunReference())
                .year(r.getYear()).month(r.getMonth())
                .periodStart(r.getPeriodStart()).periodEnd(r.getPeriodEnd())
                .status(r.getStatus().name())
                .totalEmployees(r.getTotalEmployees())
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
                .salaryCapped(d.getSalaryCapped())
                .paymentStatus(d.getPaymentStatus().name())
                .paidDate(d.getPaidDate())
                .paymentMode(d.getPaymentMode())
                .paidBy(d.getPaidBy())
                .build();
    }
}