package com.business.erp.leave.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestDto {
    @NotNull
    private Long employeeId;
    @NotNull
    private LocalDate leaveDate;
    private String reason;
}
