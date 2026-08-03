package com.business.erp.employee.dto.request;

import com.business.erp.employee.enums.EmploymentType;
import com.business.erp.employee.enums.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateEmployeeRequest {
    @Size(max = 100)
    private String name;
    @Pattern(regexp = "^[0-9]{10}$")
    private String phone;
    private String address;
    private String email;

    private Long designationId;
    private Long departmentId;
    private Long employeeCategoryId;
    private EmploymentType employmentType;
    private Long reportingManagerId;

    private LocalDate dateOfBirth;
    private Gender gender;
    @Size(max = 100)
    private String emergencyContactName;
    @Pattern(regexp = "^[0-9]{10}$")
    private String emergencyContactPhone;

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