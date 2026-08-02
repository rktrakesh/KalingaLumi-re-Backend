package com.business.erp.employee.dto.request;

import com.business.erp.employee.enums.EmployeeCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateEmployeeRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
    private String address;
    @Email
    @Size(max = 150)
    private String email;
    @NotNull
    private LocalDate joiningDate;
    @Size(max = 100)
    private String designation;

    private EmployeeCategory employeeCategory;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal currentSalary;
    private String salaryRemarks;
}