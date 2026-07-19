package com.business.erp.overtime.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OvertimeResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate overtimeDate;
    private String requestType;
    private Integer requestedMinutes;
    private Integer approvedMinutes;
    private String status;
    private String approvedBy;
    private LocalDateTime approvedDate;
    private String remarks;
    private LocalDateTime createdDate;
}
