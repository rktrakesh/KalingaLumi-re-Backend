package com.business.erp.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CheckInRequest {
    @NotNull
    private Long employeeId;
    @NotNull
    private LocalDate attendanceDate;
    @NotNull
    private LocalTime checkIn;
    private String remarks;
}
