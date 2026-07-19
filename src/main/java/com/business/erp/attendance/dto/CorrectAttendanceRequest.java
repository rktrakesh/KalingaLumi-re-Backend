package com.business.erp.attendance.dto;

import com.business.erp.attendance.entity.AttendanceRecord;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CorrectAttendanceRequest {
    private LocalTime checkIn;
    private LocalTime checkOut;
    private AttendanceRecord.AttendanceStatus status;
    @NotBlank(message = "Remarks required for correction")
    private String remarks;
}
