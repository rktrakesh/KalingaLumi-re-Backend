package com.business.erp.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignOwnerRequest {
    @NotNull
    private Long customerId;
    @NotNull
    private Long employeeId;
    @NotNull
    private LocalDate effectiveFrom;
    private String remarks;
}