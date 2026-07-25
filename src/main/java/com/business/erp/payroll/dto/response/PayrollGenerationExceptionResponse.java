package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * One employee that could not be processed during payroll generation/recalculation.
 * The run still completes for every other employee — see item 8, Exception Report.
 */
@Data
@Builder
public class PayrollGenerationExceptionResponse {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String reason;
    private String message;
}