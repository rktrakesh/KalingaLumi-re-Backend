package com.business.erp.performance.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TransferCustomersRequest {
    @NotEmpty
    private List<Long> customerIds;
    @NotNull
    private Long toEmployeeId;
    @NotNull
    private LocalDate effectiveFrom;
    private String remarks;
}