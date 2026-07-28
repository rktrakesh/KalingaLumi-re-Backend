package com.business.erp.payroll.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReopenPayrollRequest {
    @NotBlank(message = "A reason is required to reopen payroll")
    private String reason;
}