package com.business.erp.employee.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEmployeeRequest {
    @Size(max = 100)
    private String name;
    @Pattern(regexp = "^[0-9]{10}$")
    private String phone;
    private String address;
    @Size(max = 100)
    private String designation;
}
