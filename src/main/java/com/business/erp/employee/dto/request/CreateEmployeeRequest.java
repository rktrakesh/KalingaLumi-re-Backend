package com.business.erp.employee.dto.request;

import com.business.erp.employee.enums.EmploymentType;
import com.business.erp.employee.enums.Gender;
import com.business.erp.employee.entity.Employee;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateEmployeeRequest {
    /** Null preserves legacy API behavior: a login is created. New clients send an explicit choice. */
    private Boolean createLogin;
    /** Null preserves the legacy API behavior of creating an ACTIVE employee. */
    private Employee.EmployeeStatus status;
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

    @NotNull
    private Long designationId;
    private Long departmentId;

    /** Drives Overtime eligibility, Payroll's Performance Incentive integration, and the
     *  Performance Engine's own employee filters. */
    @NotNull
    private Long employeeCategoryId;
    private EmploymentType employmentType;
    private Long reportingManagerId;

    private LocalDate dateOfBirth;
    private Gender gender;
    @Size(max = 100)
    private String emergencyContactName;
    @Pattern(regexp = "^[0-9]{10}$", message = "Emergency contact phone must be 10 digits")
    private String emergencyContactPhone;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal currentSalary;
    private String salaryRemarks;

    // ---- PAN & Bank Details — optional, never mandatory ----
    @Size(max = 20)
    private String panNumber;
    @Size(max = 30)
    private String bankAccountNumber;
    @Size(max = 15)
    private String bankIfsc;
    @Size(max = 100)
    private String bankName;
    @Size(max = 100)
    private String bankAccountHolderName;
}
