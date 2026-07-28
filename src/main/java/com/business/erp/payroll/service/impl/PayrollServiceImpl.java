package com.business.erp.payroll.service.impl;

import com.business.erp.attendance.service.AttendanceService;
import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.InvalidPayrollStateException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.repository.EmployeeSalaryHistoryRepository;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.leave.service.LeaveSettlementService;
import com.business.erp.loan.service.LoanService;
import com.business.erp.monthclosing.service.MonthClosingService;
import com.business.erp.payroll.dto.request.DisbursePaymentRequest;
import com.business.erp.payroll.dto.request.GeneratePayrollRequest;
import com.business.erp.payroll.dto.response.*;
import com.business.erp.payroll.engine.PayrollCalculationContext;
import com.business.erp.payroll.engine.PayrollCalculationEngine;
import com.business.erp.payroll.engine.PayrollCalculationResult;
import com.business.erp.payroll.engine.PayrollAmounts;
import com.business.erp.payroll.engine.PayrollCalendarEngine;
import com.business.erp.payroll.engine.PayrollExceptionReportService;
import com.business.erp.payroll.engine.PayrollMetrics;
import com.business.erp.payroll.engine.PayrollPolicyService;
import com.business.erp.payroll.engine.PayrollSnapshotService;
import com.business.erp.payroll.engine.PayrollValidationService;
import com.business.erp.payroll.engine.calendar.PayrollCalendar;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import com.business.erp.payroll.engine.period.PayrollPeriod;
import com.business.erp.payroll.engine.period.PayrollPeriodFactory;
import com.business.erp.payroll.engine.validation.PayrollGenerationValidationPipeline;
import com.business.erp.payroll.engine.version.PayrollEngineVersion;
import com.business.erp.leave.dto.response.LeaveSettlementResult;
import com.business.erp.payroll.entity.PayrollCalculationLog;
import com.business.erp.payroll.entity.PayrollDetail;
import com.business.erp.payroll.entity.PayrollRun;
import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import com.business.erp.payroll.enums.PayrollStatus;
import com.business.erp.payroll.repository.PayrollCalculationLogRepository;
import com.business.erp.payroll.repository.PayrollDetailRepository;
import com.business.erp.payroll.repository.PayrollRunRepository;
import com.business.erp.payroll.service.PayrollService;
import com.business.erp.settings.enums.PayrollGenerationPolicy;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final PayrollCalculationLogRepository calculationLogRepository;
    private final EmployeeService employeeService;
    private final EmployeeSalaryHistoryRepository salaryHistoryRepository;
    private final LoanService loanService;
    private final ReferenceNumberService refService;
    private final MonthClosingService monthClosingService;
    private final CashbookService cashbookService;
    private final AuditService auditService;
    private final AttendanceService attendanceService;
    private final LeaveSettlementService leaveSettlementService;
    private final PayrollCalendarEngine calendarEngine;
    private final PayrollCalculationEngine calculationEngine;
    private final PayrollPolicyService policyService;
    private final PayrollSnapshotService snapshotService;
    private final PayrollValidationService validationService;
    private final PayrollExceptionReportService exceptionReportService;
    private final PayrollPeriodFactory payrollPeriodFactory;
    private final PayrollGenerationValidationPipeline validationPipeline;
    private final ClockProvider clockProvider;
    private final SettingsService settingsService;
    private final Logger log = LoggerFactory.getLogger(PayrollServiceImpl.class);

    // ─────────────────────────────────────────────────────────────────────────
    // GENERATE — first version for a period
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollRunResponse generate(GeneratePayrollRequest req, String generatedBy) {
        long startedAt = System.currentTimeMillis();
        log.info("PayrollServiceImpl:generate :: Payroll Generation Requested year={} month={} by={}",
                req.getYear(), req.getMonth(), generatedBy);

        PayrollPeriod period = payrollPeriodFactory.of(req.getMonth(), req.getYear());
        log.debug("PayrollServiceImpl:generate :: Payroll Period Loaded period={}", period);

        PayrollGenerationPolicy policy = PayrollGenerationPolicy.valueOf(
                settingsService.getCurrentValue(SettingKey.PAYROLL_GENERATION_POLICY));
        log.debug("PayrollServiceImpl:generate :: Payroll Policy Loaded policy={}", policy);
        log.debug("PayrollServiceImpl:generate :: Current Date={}", clockProvider.today());

        PayrollGenerationContext context = PayrollGenerationContext.builder()
                .period(period)
                .requestedBy(generatedBy)
                .generationDate(LocalDateTime.now())
                .generationPolicy(policy)
                .engineVersion(PayrollEngineVersion.CURRENT)
                .calculationVersion(1)
                .build();

        validationPipeline.validate(context); // throws + records blocked-audit if not allowed; nothing persisted yet
        log.info("PayrollServiceImpl:generate :: Payroll Generation Allowed period={}", period);

        String ref = refService.generatePayrollReference(period.getPeriodStart());
        PayrollRun run = payrollRunRepository.save(PayrollRun.builder()
                .runReference(ref).year(period.getYear()).month(period.getMonth())
                .periodStart(period.getPeriodStart()).periodEnd(period.getPeriodEnd())
                .status(PayrollStatus.DRAFT)
                .calculationVersion(1).isCurrentVersion(true)
                .generatedBy(generatedBy).generatedDate(LocalDateTime.now())
                .remarks(req.getRemarks()).build());

        PayrollSettingsSnapshot snapshot = snapshotService.captureForRun(run.getId(), generatedBy, policy);
        run.setSnapshotId(snapshot.getId());
        payrollRunRepository.save(run);
        log.debug("PayrollServiceImpl:generate :: Snapshot Loaded snapshotId={} engineVersion={}", snapshot.getId(), snapshot.getEngineVersion());

        ComputeOutcome outcome = computeAllDetails(run, snapshot, generatedBy);
        auditService.log("PAYROLL", "GENERATE", "PayrollRun", outcome.run().getId());

        long durationMs = System.currentTimeMillis() - startedAt;
        log.info("PayrollServiceImpl:generate :: SUCCESS runId={} ref={} employees={} durationMs={} engineVersion={}",
                outcome.run().getId(), ref, outcome.run().getTotalEmployees(), durationMs, PayrollEngineVersion.CURRENT);
        return toRunResponse(outcome.run(), outcome.metrics(), outcome.exceptions());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECALCULATE — new version while still pre-verification
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollRunResponse recalculate(Long runId, String actor) {
        log.info("PayrollServiceImpl:recalculate :: runId={} by={}", runId, actor);
        PayrollRun current = fetchRun(runId);
        validationService.validateRecalculate(current);
        PayrollRun newVersion = supersede(current, actor, false, null);
        PayrollSettingsSnapshot snapshot = snapshotService.getForRun(current.getId());
        newVersion.setSnapshotId(snapshot.getId());
        ComputeOutcome outcome = computeAllDetails(newVersion, snapshot, actor);
        auditService.log("PAYROLL", "RECALCULATE", "PayrollRun", outcome.run().getId());
        log.info("PayrollServiceImpl:recalculate :: SUCCESS oldRunId={} newRunId={} version={}",
                current.getId(), outcome.run().getId(), outcome.run().getCalculationVersion());
        return toRunResponse(outcome.run(), outcome.metrics(), outcome.exceptions());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY / APPROVE / LOCK
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollRunResponse verify(Long runId, String actor, String remarks) {
        log.info("PayrollServiceImpl:verify :: runId={} by={}", runId, actor);
        PayrollRun run = fetchRun(runId);
        validationService.validateVerify(run);
        run.setStatus(PayrollStatus.VERIFIED);
        run.setVerifiedBy(actor);
        run.setVerifiedDate(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) run.setRemarks(remarks);
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log("PAYROLL", "VERIFY", "PayrollRun", runId);
        return toRunResponse(saved);
    }

    @Override
    @Transactional
    public PayrollRunResponse approve(Long runId, String actor, String remarks) {
        log.info("PayrollServiceImpl:approve :: runId={} by={}", runId, actor);
        PayrollRun run = fetchRun(runId);
        validationService.validateApprove(run);
        run.setStatus(PayrollStatus.APPROVED);
        run.setApprovedBy(actor);
        run.setApprovedDate(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) run.setRemarks(remarks);
        PayrollRun saved = payrollRunRepository.save(run);

        // Freeze attendance for the period — this is the moment the payroll "locks in".
        attendanceService.lockForPayroll(run.getPeriodStart(), run.getPeriodEnd(), run.getId());

        auditService.log("PAYROLL", "APPROVE", "PayrollRun", runId);
        log.info("PayrollServiceImpl:approve :: SUCCESS runId={} attendanceLocked={}..{}",
                runId, run.getPeriodStart(), run.getPeriodEnd());
        return toRunResponse(saved);
    }

    @Override
    @Transactional
    public PayrollRunResponse reopen(Long runId, String actor, String reason) {
        log.info("PayrollServiceImpl:reopen :: runId={} by={} reason={}", runId, actor, reason);
        PayrollRun current = fetchRun(runId);
        validationService.validateReopen(current);

        // Release the attendance freeze this run applied (no-op if it never reached APPROVED).
        attendanceService.unlockForPayroll(current.getId());

        PayrollRun newVersion = supersede(current, actor, true, reason);
        PayrollSettingsSnapshot snapshot = snapshotService.getForRun(current.getId());
        newVersion.setSnapshotId(snapshot.getId());
        ComputeOutcome outcome = computeAllDetails(newVersion, snapshot, actor);

        auditService.log("PAYROLL", "REOPEN", "PayrollRun", outcome.run().getId());
        log.info("PayrollServiceImpl:reopen :: SUCCESS oldRunId={} newRunId={}", current.getId(), outcome.run().getId());
        return toRunResponse(outcome.run(), outcome.metrics(), outcome.exceptions());
    }

    @Override
    @Transactional
    public PayrollRunResponse lockRun(Long runId, String lockedBy) {
        log.info("PayrollServiceImpl:lockRun :: runId={} by={}", runId, lockedBy);
        PayrollRun run = fetchRun(runId);
        validationService.validateLock(run);
        run.setStatus(PayrollStatus.LOCKED);
        run.setLockedBy(lockedBy);
        run.setLockedDate(LocalDateTime.now());
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log("PAYROLL", "LOCK", "PayrollRun", runId);
        log.info("PayrollServiceImpl:lockRun :: SUCCESS runId={}", runId);
        return toRunResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DISBURSEMENT
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PayrollDetailResponse disburseOne(Long detailId, DisbursePaymentRequest req, String paidBy) {
        log.info("PayrollServiceImpl:disburseOne :: detailId={} mode={} by={}", detailId, req.getPaymentMode(), paidBy);
        PayrollDetail detail = payrollDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDetail", detailId));
        PayrollRun run = detail.getPayrollRun();
        validationService.validateProcess(run);

        if (detail.getPaymentStatus() == PayrollDetail.PaymentStatus.PAID)
            throw new BusinessException(detail.getEmployeeName() + " salary is already marked as PAID");

        detail.setPaymentStatus(PayrollDetail.PaymentStatus.PAID);
        detail.setPaidDate(LocalDate.now());
        detail.setPaidBy(paidBy);
        detail.setPaymentMode(req.getPaymentMode().name());
        PayrollDetail saved = payrollDetailRepository.save(detail);

        cashbookService.recordSalaryPayment(detail.getNetSalary(), run.getRunReference(), detail.getEmployeeName());
        transitionToProcessedOrPaid(run);

        auditService.log("PAYROLL", "DISBURSE", "PayrollDetail", detailId);
        log.info("PayrollServiceImpl:disburseOne :: SUCCESS emp={} amount={} mode={}",
                detail.getEmployeeCode(), detail.getNetSalary(), req.getPaymentMode());
        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public void disburseAll(Long runId, DisbursePaymentRequest req, String paidBy) {
        log.info("PayrollServiceImpl:disburseAll :: runId={} mode={} by={}", runId, req.getPaymentMode(), paidBy);
        PayrollRun run = fetchRun(runId);
        validationService.validateProcess(run);

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
            cashbookService.recordSalaryPayment(detail.getNetSalary(), run.getRunReference(), detail.getEmployeeName());
            count++;
        }
        transitionToProcessedOrPaid(run);

        auditService.log("PAYROLL", "DISBURSE_ALL", "PayrollRun", runId);
        log.info("PayrollServiceImpl:disburseAll :: SUCCESS runId={} disbursed={} employees", runId, count);
    }

    private void transitionToProcessedOrPaid(PayrollRun run) {
        if (run.getStatus() == PayrollStatus.APPROVED) {
            run.setStatus(PayrollStatus.PROCESSED);
            run.setProcessedBy(run.getApprovedBy());
            run.setProcessedDate(LocalDateTime.now());
        }
        long remaining = payrollDetailRepository.countByPayrollRunIdAndPaymentStatus(run.getId(), PayrollDetail.PaymentStatus.PENDING);
        if (remaining == 0) run.setStatus(PayrollStatus.PAID);
        payrollRunRepository.save(run);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READS
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getAllRuns() {
        return payrollRunRepository.findByIsCurrentVersionTrueOrderByYearDescMonthDesc()
                .stream().map(this::toRunResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRunResponse getRun(Long runId) {
        return toRunResponse(fetchRun(runId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getVersionHistory(int year, int month) {
        return payrollRunRepository.findByYearAndMonthOrderByCalculationVersionAsc(year, month)
                .stream().map(this::toRunResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollDetailResponse> getDetails(Long runId) {
        return payrollDetailRepository.findByPayrollRunId(runId)
                .stream().map(this::toDetailResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDetailResponse getEmployeePayslip(Long empId, int year, int month) {
        return payrollDetailRepository
                .findByPayrollRun_YearAndPayrollRun_MonthAndPayrollRun_IsCurrentVersionTrueAndEmployeeId(year, month, empId)
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payslip not found for employee " + empId + " for " + year + "-" + month));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollCalculationLogResponse> getCalculationLogs(Long runId) {
        List<PayrollCalculationLog> logs = calculationLogRepository.findByPayrollRunId(runId);
        Map<Long, Employee> employeesById = employeeService.getEmployeesByIds(
                logs.stream().map(PayrollCalculationLog::getEmployeeId).distinct().collect(Collectors.toList()));
        return logs.stream().map(l -> toCalcLogResponse(l, employeesById.get(l.getEmployeeId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollCalculationLogResponse> getEmployeeCalculationHistory(Long employeeId) {
        Employee emp = employeeService.getEmployee(employeeId);
        return calculationLogRepository.findByEmployeeIdOrderByCalculatedDateDesc(employeeId)
                .stream().map(l -> toCalcLogResponse(l, emp)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDashboardResponse getDashboard(Long runId) {
        PayrollRun run = fetchRun(runId);
        List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(runId);

        BigDecimal totalBasic = details.stream().map(PayrollDetail::getBaseSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOt = details.stream().map(d -> nz(d.getOvertimePay())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeeklyOff = details.stream().map(d -> nz(d.getWeeklyOffPay())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHoliday = details.stream().map(d -> nz(d.getHolidayOtPay())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEncashment = details.stream().map(d -> nz(d.getLeaveEncashmentAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLop = details.stream().map(d -> nz(d.getLossOfPayAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotal = details.stream().map(PayrollDetail::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);

        return PayrollDashboardResponse.builder()
                .payrollRunId(runId).year(run.getYear()).month(run.getMonth()).status(run.getStatus().name())
                .totalEmployees(details.size())
                .totalBasicSalary(totalBasic.setScale(2, RoundingMode.HALF_UP))
                .totalOvertime(totalOt.setScale(2, RoundingMode.HALF_UP))
                .totalWeeklyOffAmount(totalWeeklyOff.setScale(2, RoundingMode.HALF_UP))
                .totalHolidayPay(totalHoliday.setScale(2, RoundingMode.HALF_UP))
                .totalLeaveEncashment(totalEncashment.setScale(2, RoundingMode.HALF_UP))
                .totalLossOfPay(totalLop.setScale(2, RoundingMode.HALF_UP))
                .grandTotal(grandTotal.setScale(2, RoundingMode.HALF_UP))
                .paidCount(payrollDetailRepository.countByPayrollRunIdAndPaymentStatus(runId, PayrollDetail.PaymentStatus.PAID))
                .pendingCount(payrollDetailRepository.countByPayrollRunIdAndPaymentStatus(runId, PayrollDetail.PaymentStatus.PENDING))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollExceptionResponse> getExceptionReport(int year, int month) {
        return exceptionReportService.generate(year, month);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Marks {@code current} as no longer current and inserts+returns the next version's (empty) run row. */
    private PayrollRun supersede(PayrollRun current, String actor, boolean isReopen, String reopenReason) {
        current.setIsCurrentVersion(false);
        if (isReopen) {
            current.setReopenedBy(actor);
            current.setReopenedDate(LocalDateTime.now());
            current.setReopenReason(reopenReason);
        }
        payrollRunRepository.save(current);

        // Undo whatever the superseded version consumed/settled so the new version starts clean.
        for (PayrollCalculationLog calcLog : calculationLogRepository.findByPayrollRunId(current.getId())) {
            if (calcLog.getAutomaticPaidLeaveDays() != null && calcLog.getAutomaticPaidLeaveDays() > 0) {
                leaveSettlementService.restoreAutomaticLeave(
                        calcLog.getEmployeeId(), current.getYear(), current.getMonth(), calcLog.getAutomaticPaidLeaveDays());
            }
            leaveSettlementService.reverseSettlement(calcLog.getEmployeeId(), current.getYear(), current.getMonth());
        }

        int nextVersion = current.getCalculationVersion() + 1;
        String ref = current.getRunReference() + "-V" + nextVersion;
        return payrollRunRepository.save(PayrollRun.builder()
                .runReference(ref).year(current.getYear()).month(current.getMonth())
                .periodStart(current.getPeriodStart()).periodEnd(current.getPeriodEnd())
                .status(PayrollStatus.DRAFT)
                .calculationVersion(nextVersion).isCurrentVersion(true)
                .previousRunId(current.getId())
                .generatedBy(actor).generatedDate(LocalDateTime.now())
                .build());
    }

    /** Bundles what computeAllDetails produces beyond the persisted run — see items 7/8. */
    private record ComputeOutcome(PayrollRun run, PayrollMetrics metrics,
                                  List<PayrollGenerationExceptionResponse> exceptions) {
    }

    /** Builds the calendar, settles leave, computes salary for every active employee, and persists in batch. */
    private ComputeOutcome computeAllDetails(PayrollRun run, PayrollSettingsSnapshot snapshot, String actor) {
        long startedAt = System.currentTimeMillis();
        log.info("PayrollServiceImpl:computeAllDetails :: START runId={} period={}..{}",
                run.getId(), run.getPeriodStart(), run.getPeriodEnd());
        payrollDetailRepository.deleteByPayrollRunId(run.getId()); // safe: only ever empty for a fresh version

        // Holidays are identical for every employee in the run — load once, not per employee (item 5).
        Map<LocalDate, com.business.erp.holiday.entity.Holiday> holidaysByDate =
                calendarEngine.loadHolidays(run.getPeriodStart(), run.getPeriodEnd());

        List<Employee> activeEmps = employeeService.getActiveEmployees();
        List<PayrollDetail> detailsToSave = new ArrayList<>(activeEmps.size());
        List<PayrollCalculationResult> resultsInOrder = new ArrayList<>(activeEmps.size());
        List<PayrollGenerationExceptionResponse> exceptions = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;

        for (Employee emp : activeEmps) {
            try {
                if (emp.getCurrentSalary() == null || emp.getCurrentSalary().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("NO_SALARY: employee has no salary configured");
                }

                PayrollCalendar calendar = calendarEngine.buildCalendar(
                        emp.getId(), run.getPeriodStart(), run.getPeriodEnd(), snapshot, holidaysByDate);

                BigDecimal salary = salaryHistoryRepository.findSalaryAsOf(emp.getId(), run.getPeriodStart())
                        .map(h -> h.getSalary()).orElse(emp.getCurrentSalary());
                BigDecimal hourlyRate = policyService.computeHourlyRate(salary, snapshot);
                BigDecimal dailySalary = policyService.computeDailySalary(salary, snapshot);

                LeaveSettlementResult settlement = leaveSettlementService.settleUnusedLeave(
                        run.getId(), emp.getId(), run.getYear(), run.getMonth(),
                        policyService.unusedLeavePolicy(snapshot), policyService.leaveCarryForwardLimit(snapshot),
                        policyService.isLeaveEncashmentEnabled(snapshot), dailySalary, actor);

                PayrollCalculationContext context = PayrollCalculationContext.builder()
                        .employee(emp).payrollRun(run).snapshot(snapshot).calendar(calendar)
                        .salary(salary)
                        .standardWorkingDays(policyService.standardWorkingDays(snapshot))
                        .workingHoursPerDay(policyService.workingHoursPerDay(snapshot))
                        .hourlyRate(hourlyRate)
                        .leaveEncashmentDays(BigDecimal.valueOf(settlement.getEncashedDays()))
                        .leaveEncashmentAmount(settlement.getEncashmentAmount())
                        .build();

                PayrollCalculationResult result = calculationEngine.calculate(context);

                detailsToSave.add(buildDetail(run, emp, snapshot, salary, result, actor));
                resultsInOrder.add(result);
                totalGross = totalGross.add(result.getAmounts().getGrossSalary());
            } catch (Exception ex) {
                // One employee's failure must never abort the whole run (item 8).
                log.warn("PayrollServiceImpl:computeAllDetails :: SKIPPED empId={} runId={} reason={}",
                        emp.getId(), run.getId(), ex.getMessage());
                exceptions.add(PayrollGenerationExceptionResponse.builder()
                        .employeeId(emp.getId()).employeeCode(emp.getEmployeeCode()).employeeName(emp.getName())
                        .reason(classifyFailure(ex)).message(ex.getMessage())
                        .build());
            }
        }

        // Batch-insert details (single flush), then build calculation logs now that IDs are assigned.
        List<PayrollDetail> savedDetails = payrollDetailRepository.saveAll(detailsToSave);
        List<PayrollCalculationLog> logsToSave = new ArrayList<>(savedDetails.size());
        for (int i = 0; i < savedDetails.size(); i++) {
            PayrollDetail savedDetail = savedDetails.get(i);
            PayrollCalculationResult result = resultsInOrder.get(i);
            logsToSave.add(buildCalcLog(run, savedDetail.getEmployee(), result, savedDetail.getId(), actor));
        }
        calculationLogRepository.saveAll(logsToSave);

        BigDecimal actualTotalNet = savedDetails.stream().map(PayrollDetail::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        run.setTotalEmployees(savedDetails.size());
        run.setTotalGross(totalGross.setScale(2, RoundingMode.HALF_UP));
        run.setTotalNet(actualTotalNet.setScale(2, RoundingMode.HALF_UP));
        run.setStatus(PayrollStatus.CALCULATED);
        PayrollRun saved = payrollRunRepository.save(run);

        long durationMs = System.currentTimeMillis() - startedAt;
        PayrollMetrics metrics = buildMetrics(savedDetails, durationMs, exceptions.size());

        log.info("PayrollServiceImpl:computeAllDetails :: DONE runId={} month={}-{} processed={} skipped={} " +
                        "durationMs={} totalPayroll={} totalOt={} totalWeeklyOff={} totalHolidayOt={} warnings={}",
                run.getId(), run.getYear(), run.getMonth(), metrics.getEmployeesProcessed(), metrics.getEmployeesSkipped(),
                durationMs, metrics.getTotalPayroll(), metrics.getTotalOt(), metrics.getTotalWeeklyOffPay(),
                metrics.getTotalHolidayOt(), exceptions.size());

        return new ComputeOutcome(saved, metrics, exceptions);
    }

    private PayrollMetrics buildMetrics(List<PayrollDetail> details, long durationMs, int skipped) {
        int processed = details.size();
        BigDecimal totalPayroll = details.stream().map(PayrollDetail::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOt = details.stream().map(d -> nz(d.getOvertimePay())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHolidayOt = details.stream().map(d -> nz(d.getHolidayOtPay())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeeklyOff = details.stream().map(d -> nz(d.getWeeklyOffPay())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEncashment = details.stream().map(d -> nz(d.getLeaveEncashmentAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLop = details.stream().map(d -> nz(d.getLossOfPayAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        double avgOtMinutes = processed == 0 ? 0 : details.stream().mapToInt(d -> nzInt(d.getOvertimeMinutes())).average().orElse(0);
        double avgLeaveDays = processed == 0 ? 0 : details.stream().mapToInt(d -> nzInt(d.getPaidLeaveDays())).average().orElse(0);

        return PayrollMetrics.builder()
                .employeesProcessed(processed).employeesSkipped(skipped).executionDurationMillis(durationMs)
                .totalPayroll(totalPayroll.setScale(2, RoundingMode.HALF_UP))
                .totalOt(totalOt.setScale(2, RoundingMode.HALF_UP))
                .totalHolidayOt(totalHolidayOt.setScale(2, RoundingMode.HALF_UP))
                .totalWeeklyOffPay(totalWeeklyOff.setScale(2, RoundingMode.HALF_UP))
                .totalLeaveEncashment(totalEncashment.setScale(2, RoundingMode.HALF_UP))
                .totalLossOfPay(totalLop.setScale(2, RoundingMode.HALF_UP))
                .averageSalary(processed == 0 ? BigDecimal.ZERO
                        : totalPayroll.divide(BigDecimal.valueOf(processed), 2, RoundingMode.HALF_UP))
                .averageOt(BigDecimal.valueOf(avgOtMinutes / 60.0).setScale(2, RoundingMode.HALF_UP))
                .averageLeaveDays(BigDecimal.valueOf(avgLeaveDays).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private String classifyFailure(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.startsWith("NO_SALARY")) return "NO_SALARY";
        if (ex instanceof InvalidPayrollStateException) return "VALIDATION_FAILURE";
        return "UNKNOWN";
    }

    private PayrollMetricsResponse toMetricsResponse(PayrollMetrics m) {
        return PayrollMetricsResponse.builder()
                .employeesProcessed(m.getEmployeesProcessed()).employeesSkipped(m.getEmployeesSkipped())
                .executionDurationMillis(m.getExecutionDurationMillis())
                .totalPayroll(m.getTotalPayroll()).totalOt(m.getTotalOt())
                .totalHolidayOt(m.getTotalHolidayOt()).totalWeeklyOffPay(m.getTotalWeeklyOffPay())
                .totalLeaveEncashment(m.getTotalLeaveEncashment()).totalLossOfPay(m.getTotalLossOfPay())
                .averageSalary(m.getAverageSalary()).averageOt(m.getAverageOt()).averageLeaveDays(m.getAverageLeaveDays())
                .build();
    }

    private int nzInt(Integer v) {
        return v != null ? v : 0;
    }

    private PayrollDetail buildDetail(PayrollRun run, Employee emp, PayrollSettingsSnapshot snapshot,
                                      BigDecimal salary, PayrollCalculationResult result, String actor) {
        PayrollAmounts amounts = result.getAmounts();
        BigDecimal[] loanDeductions = loanService.calculateAndPostDeductions(
                emp.getId(), run.getPeriodStart(), amounts.getGrossSalary(), actor);
        BigDecimal loanInterest = loanDeductions[0];
        BigDecimal loanPrincipal = loanDeductions[1];
        BigDecimal totalDeductions = loanInterest.add(loanPrincipal);
        BigDecimal netAfterLoans = amounts.getNetSalary().subtract(totalDeductions).max(BigDecimal.ZERO);
        boolean capped = totalDeductions.compareTo(amounts.getNetSalary()) > 0;

        // PayrollDetail is populated ONLY from PayrollCalculationResult (+ loan deductions, a separate concern).
        return PayrollDetail.builder()
                .payrollRun(run).employee(emp)
                .calculationVersion(run.getCalculationVersion())
                .employeeCode(emp.getEmployeeCode()).employeeName(emp.getName())
                .baseSalary(salary)
                .standardWorkDays(snapshot.getStandardWorkingDays())
                .standardWorkHours(snapshot.getWorkingHoursPerDay())
                .hourlyRate(amounts.getHourlyRate())
                .presentDays(result.getWorkedDays())
                .weeklyOffDays(result.getWeeklyOffCount()).weeklyOffWorkedDays(result.getWeeklyOffWorkedCount())
                .holidayDays(result.getHolidayCount()).holidayWorkedDays(result.getHolidayWorkedCount())
                .automaticPaidLeaveDays(result.getAutomaticPaidLeaveCount())
                .absentDays(result.getAbsentCount())
                .workedMinutes(result.getWorkedDays() * snapshot.getWorkingHoursPerDay() * 60)
                .paidLeaveDays(result.getPaidLeaveCount() + result.getAutomaticPaidLeaveCount())
                .overtimeMinutes(result.getOtMinutes())
                .overtimeMultiplier(snapshot.getOvertimeMultiplier())
                .holidayOtMinutes(result.getHolidayOtMinutes())
                .weeklyOffOtMinutes(result.getWeeklyOffOtMinutes())
                .weeklyOffMultiplier(snapshot.getWeeklyOffMultiplier())
                .holidayOtMultiplier(snapshot.getHolidayOtMultiplier())
                .weeklyOffPay(amounts.getWeeklyOffAmount())
                .holidayOtPay(amounts.getHolidayOtAmount())
                .overtimePay(amounts.getOtAmount())
                .leaveEncashmentDays(result.getLeaveEncashmentDays())
                .leaveEncashmentAmount(amounts.getLeaveEncashmentAmount())
                .lossOfPayAmount(amounts.getLossOfPayAmount())
                .grossSalary(amounts.getGrossSalary())
                .loanInterestDeduction(loanInterest).loanPrincipalDeduction(loanPrincipal)
                .totalDeductions(totalDeductions).netSalary(netAfterLoans)
                .salaryCapped(capped)
                .createdBy(actor).build();
    }

    private PayrollCalculationLog buildCalcLog(PayrollRun run, Employee emp, PayrollCalculationResult result,
                                               Long detailId, String actor) {
        PayrollAmounts amounts = result.getAmounts();
        return PayrollCalculationLog.builder()
                .payrollRunId(run.getId()).payrollDetailId(detailId).employeeId(emp.getId())
                .calculationVersion(run.getCalculationVersion())
                .monthlySalary(amounts.getBasicSalary()).hourlyRate(amounts.getHourlyRate())
                .presentDays(result.getWorkedDays())
                .weeklyOffDays(result.getWeeklyOffCount()).weeklyOffWorkedDays(result.getWeeklyOffWorkedCount())
                .holidayDays(result.getHolidayCount()).holidayWorkedDays(result.getHolidayWorkedCount())
                .paidLeaveDays(result.getPaidLeaveCount()).automaticPaidLeaveDays(result.getAutomaticPaidLeaveCount())
                .absentDays(result.getAbsentCount())
                .approvedOtMinutes(result.getOtMinutes())
                .holidayOtMinutes(result.getHolidayOtMinutes()).weeklyOffOtMinutes(result.getWeeklyOffOtMinutes())
                .basicSalaryAmount(amounts.getBasicSalary())
                .overtimeAmount(amounts.getOtAmount()).weeklyOffPayAmount(amounts.getWeeklyOffAmount())
                .holidayOtAmount(amounts.getHolidayOtAmount())
                .leaveEncashmentDays(result.getLeaveEncashmentDays()).leaveEncashmentAmount(amounts.getLeaveEncashmentAmount())
                .lossOfPayAmount(amounts.getLossOfPayAmount())
                .grossSalary(amounts.getGrossSalary()).finalNetSalary(amounts.getNetSalary())
                .calculatedBy(actor).calculatedDate(LocalDateTime.now())
                .calculationBreakdown(String.join("\n", result.getBreakdown()))
                .engineVersion(result.getEngineVersion())
                .build();
    }

    private PayrollRun fetchRun(Long runId) {
        return payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", runId));
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private PayrollRunResponse toRunResponse(PayrollRun r) {
        return toRunResponse(r, null, null);
    }

    private PayrollRunResponse toRunResponse(PayrollRun r, PayrollMetrics metrics,
                                             List<PayrollGenerationExceptionResponse> exceptions) {
        return PayrollRunResponse.builder()
                .id(r.getId()).runReference(r.getRunReference())
                .year(r.getYear()).month(r.getMonth())
                .periodStart(r.getPeriodStart()).periodEnd(r.getPeriodEnd())
                .status(r.getStatus().name())
                .calculationVersion(r.getCalculationVersion()).isCurrentVersion(r.getIsCurrentVersion())
                .previousRunId(r.getPreviousRunId())
                .totalEmployees(r.getTotalEmployees())
                .totalGross(r.getTotalGross()).totalNet(r.getTotalNet())
                .generatedBy(r.getGeneratedBy()).generatedDate(r.getGeneratedDate())
                .verifiedBy(r.getVerifiedBy()).verifiedDate(r.getVerifiedDate())
                .approvedBy(r.getApprovedBy()).approvedDate(r.getApprovedDate())
                .processedBy(r.getProcessedBy()).processedDate(r.getProcessedDate())
                .lockedBy(r.getLockedBy()).lockedDate(r.getLockedDate())
                .reopenedBy(r.getReopenedBy()).reopenedDate(r.getReopenedDate()).reopenReason(r.getReopenReason())
                .remarks(r.getRemarks())
                .metrics(metrics != null ? toMetricsResponse(metrics) : null)
                .generationExceptions(exceptions != null ? exceptions : List.of())
                .build();
    }

    private PayrollDetailResponse toDetailResponse(PayrollDetail d) {
        return PayrollDetailResponse.builder()
                .id(d.getId()).employeeId(d.getEmployee().getId())
                .employeeCode(d.getEmployeeCode()).employeeName(d.getEmployeeName())
                .calculationVersion(d.getCalculationVersion())
                .baseSalary(d.getBaseSalary()).standardWorkDays(d.getStandardWorkDays())
                .standardWorkHours(d.getStandardWorkHours()).hourlyRate(d.getHourlyRate())
                .presentDays(d.getPresentDays())
                .weeklyOffDays(d.getWeeklyOffDays()).weeklyOffWorkedDays(d.getWeeklyOffWorkedDays())
                .holidayDays(d.getHolidayDays()).holidayWorkedDays(d.getHolidayWorkedDays())
                .paidLeaveDays(d.getPaidLeaveDays()).automaticPaidLeaveDays(d.getAutomaticPaidLeaveDays())
                .absentDays(d.getAbsentDays())
                .workedMinutes(d.getWorkedMinutes())
                .overtimeMinutes(d.getOvertimeMinutes()).overtimeMultiplier(d.getOvertimeMultiplier())
                .holidayOtMinutes(d.getHolidayOtMinutes()).weeklyOffOtMinutes(d.getWeeklyOffOtMinutes())
                .weeklyOffMultiplier(d.getWeeklyOffMultiplier()).holidayOtMultiplier(d.getHolidayOtMultiplier())
                .weeklyOffPay(d.getWeeklyOffPay()).holidayOtPay(d.getHolidayOtPay()).overtimePay(d.getOvertimePay())
                .leaveEncashmentDays(d.getLeaveEncashmentDays()).leaveEncashmentAmount(d.getLeaveEncashmentAmount())
                .lossOfPayAmount(d.getLossOfPayAmount())
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

    private PayrollCalculationLogResponse toCalcLogResponse(PayrollCalculationLog l, Employee emp) {
        return PayrollCalculationLogResponse.builder()
                .id(l.getId()).payrollRunId(l.getPayrollRunId()).employeeId(l.getEmployeeId())
                .employeeCode(emp != null ? emp.getEmployeeCode() : null)
                .employeeName(emp != null ? emp.getName() : null)
                .calculationVersion(l.getCalculationVersion())
                .monthlySalary(l.getMonthlySalary()).hourlyRate(l.getHourlyRate())
                .presentDays(l.getPresentDays())
                .weeklyOffDays(l.getWeeklyOffDays()).weeklyOffWorkedDays(l.getWeeklyOffWorkedDays())
                .holidayDays(l.getHolidayDays()).holidayWorkedDays(l.getHolidayWorkedDays())
                .paidLeaveDays(l.getPaidLeaveDays()).automaticPaidLeaveDays(l.getAutomaticPaidLeaveDays())
                .absentDays(l.getAbsentDays())
                .approvedOtMinutes(l.getApprovedOtMinutes())
                .holidayOtMinutes(l.getHolidayOtMinutes()).weeklyOffOtMinutes(l.getWeeklyOffOtMinutes())
                .basicSalaryAmount(l.getBasicSalaryAmount())
                .overtimeAmount(l.getOvertimeAmount()).weeklyOffPayAmount(l.getWeeklyOffPayAmount())
                .holidayOtAmount(l.getHolidayOtAmount())
                .leaveEncashmentDays(l.getLeaveEncashmentDays()).leaveEncashmentAmount(l.getLeaveEncashmentAmount())
                .lossOfPayAmount(l.getLossOfPayAmount())
                .grossSalary(l.getGrossSalary()).finalNetSalary(l.getFinalNetSalary())
                .calculatedBy(l.getCalculatedBy()).calculatedDate(l.getCalculatedDate())
                .breakdown(l.getCalculationBreakdown() != null
                        ? java.util.Arrays.asList(l.getCalculationBreakdown().split("\n")) : java.util.List.of())
                .build();
    }
}