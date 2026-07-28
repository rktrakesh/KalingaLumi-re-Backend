package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollRunResponse {
    private Long id;
    private String runReference;
    private int year;
    private int month;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;

    private Integer calculationVersion;
    private Boolean isCurrentVersion;
    private Long previousRunId;

    private Integer totalEmployees;
    private BigDecimal totalGross;
    private BigDecimal totalNet;

    private String generatedBy;
    private LocalDateTime generatedDate;
    private String verifiedBy;
    private LocalDateTime verifiedDate;
    private String approvedBy;
    private LocalDateTime approvedDate;
    private String processedBy;
    private LocalDateTime processedDate;
    private String lockedBy;
    private LocalDateTime lockedDate;
    private String reopenedBy;
    private LocalDateTime reopenedDate;
    private String reopenReason;

    private String remarks;

    /** Present only on the response of generate/recalculate/reopen — see item 7. */
    private PayrollMetricsResponse metrics;
    /** Employees skipped during this computation, with reasons — see item 8. Empty list if none. */
    private java.util.List<PayrollGenerationExceptionResponse> generationExceptions;
}