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
    private String status;

    // ---- Personal Information ----
    private String name;
    private String phone;
    private String email;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
    private String emergencyContactName;
    private String emergencyContactPhone;

    // ---- Employment Information ----
    private LocalDate joiningDate;
    private String designation;
    private Long designationId;
    private String designationCode;
    private String designationName;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private Long employeeCategoryId;
    private String employeeCategoryCode;
    private String employeeCategoryName;
    private String employmentType;
    private Long reportingManagerId;
    private String reportingManagerName;

    // ---- Payroll Information ----
    private BigDecimal currentSalary;
    private String panNumber;
    private String bankAccountNumber;
    private String bankIfsc;
    private String bankName;
    private String bankAccountHolderName;

    private String createdBy;
    private LocalDateTime createdDate;
}