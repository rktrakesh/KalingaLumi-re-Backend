package com.business.erp.overtime.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConvertLeaveToOTRequest {
    @NotNull
    private Long employeeId;
    @NotNull
    private Integer year;
    @NotNull
    private Integer month;
    @NotNull
    private Integer unusedLeaveDays;
    private String remarks;
}
