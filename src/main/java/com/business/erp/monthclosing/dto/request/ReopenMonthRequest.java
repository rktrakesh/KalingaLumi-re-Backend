package com.business.erp.monthclosing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReopenMonthRequest {
    @NotNull
    private Integer year;
    @NotNull
    private Integer month;
    @NotBlank(message = "Remarks required to reopen month")
    private String remarks;
}
