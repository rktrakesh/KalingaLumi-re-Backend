package com.business.erp.payroll.service;

import com.business.erp.payroll.dto.request.DisbursePaymentRequest;
import com.business.erp.payroll.dto.request.GeneratePayrollRequest;
import com.business.erp.payroll.dto.response.*;

import java.util.List;

public interface PayrollService {

    // ── Lifecycle: DRAFT -> CALCULATED -> VERIFIED -> APPROVED -> PROCESSED -> PAID -> LOCKED ──
    PayrollRunResponse generate(GeneratePayrollRequest request, String generatedBy);

    /** Creates a new calculation version for the CURRENT (still un-verified) run and recomputes it. */
    PayrollRunResponse recalculate(Long runId, String actor);

    PayrollRunResponse verify(Long runId, String actor, String remarks);

    PayrollRunResponse approve(Long runId, String actor, String remarks);

    /** Reopens a VERIFIED/APPROVED/PROCESSED run: creates a new version, unlocks attendance, reverses leave settlement. */
    PayrollRunResponse reopen(Long runId, String actor, String reason);

    PayrollRunResponse lockRun(Long runId, String lockedBy);

    // ── Disbursement ─────────────────────────────────────────────────────
    PayrollDetailResponse disburseOne(Long detailId, DisbursePaymentRequest req, String paidBy);

    void disburseAll(Long runId, DisbursePaymentRequest req, String paidBy);

    // ── Reads ────────────────────────────────────────────────────────────
    List<PayrollRunResponse> getAllRuns();

    PayrollRunResponse getRun(Long runId);

    /** Full version history (oldest to newest) for a period. */
    List<PayrollRunResponse> getVersionHistory(int year, int month);

    List<PayrollDetailResponse> getDetails(Long runId);

    PayrollDetailResponse getEmployeePayslip(Long empId, int year, int month);

    List<PayrollCalculationLogResponse> getCalculationLogs(Long runId);

    /** Full calculation history for one employee across every version ever run — for dispute resolution. */
    List<PayrollCalculationLogResponse> getEmployeeCalculationHistory(Long employeeId);

    PayrollDashboardResponse getDashboard(Long runId);

    List<PayrollExceptionResponse> getExceptionReport(int year, int month);
}