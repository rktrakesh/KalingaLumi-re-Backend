package com.business.erp.employee.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String name;
    private String phone;
    private String address;
    private LocalDate joiningDate;
    private String designation;
    private BigDecimal currentSalary;
    private String status;
    private String createdBy;
    private LocalDateTime createdDate;
}
