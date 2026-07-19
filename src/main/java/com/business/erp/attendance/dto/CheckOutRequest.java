package com.business.erp.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CheckOutRequest {
    @NotNull
    private LocalTime checkOut;
    private String remarks;
}
