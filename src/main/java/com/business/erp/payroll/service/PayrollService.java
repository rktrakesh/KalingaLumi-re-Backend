package com.business.erp.payroll.service;

import com.business.erp.payroll.dto.request.DisbursePaymentRequest;
import com.business.erp.payroll.dto.request.GeneratePayrollRequest;
import com.business.erp.payroll.dto.response.PayrollDetailResponse;
import com.business.erp.payroll.dto.response.PayrollRunResponse;

import java.util.List;

public interface PayrollService {
    PayrollRunResponse generate(GeneratePayrollRequest request, String generatedBy);

    PayrollRunResponse regenerate(Long runId, String generatedBy);

    List<PayrollRunResponse> getAllRuns();

    PayrollRunResponse getRun(Long runId);

    List<PayrollDetailResponse> getDetails(Long runId);

    PayrollDetailResponse getEmployeePayslip(Long empId, int year, int month);

    PayrollDetailResponse disburseOne(Long detailId, DisbursePaymentRequest req, String paidBy);

    void disburseAll(Long runId, DisbursePaymentRequest req, String paidBy);

    PayrollRunResponse lockRun(Long runId, String lockedBy);
}