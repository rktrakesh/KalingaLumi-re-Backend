package com.business.erp.performance.dto.request;

import com.business.erp.performance.enums.VisitOutcome;
import com.business.erp.performance.enums.VisitPurpose;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LogCustomerVisitRequest {
    @NotNull
    private Long customerId;
    @NotNull
    private Long visitedByEmployeeId;
    @NotNull
    private LocalDate visitDate;
    @NotNull
    private VisitPurpose visitPurpose;
    @NotNull
    private VisitOutcome visitOutcome;
    private String remarks;
}