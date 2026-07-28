package com.business.erp.overtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReopenOvertimeRequest {
    @NotBlank(message = "A reason is required to reopen an approved overtime request")
    private String reason;
}