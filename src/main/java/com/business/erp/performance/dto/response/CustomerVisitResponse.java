package com.business.erp.performance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerVisitResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long visitedByEmployeeId;
    private String visitedByEmployeeName;
    private LocalDate visitDate;
    private String visitPurpose;
    private String visitOutcome;
    private String remarks;
}