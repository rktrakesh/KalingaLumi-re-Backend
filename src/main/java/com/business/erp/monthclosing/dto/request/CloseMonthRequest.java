package com.business.erp.monthclosing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloseMonthRequest {
    @NotNull
    @Min(2020)
    private Integer year;
    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;
}
