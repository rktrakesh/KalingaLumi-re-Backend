package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PayrollExceptionResponse {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private List<String> issues;
    private List<LocalDate> affectedDates;
}