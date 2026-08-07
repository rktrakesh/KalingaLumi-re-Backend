package com.business.erp.employee.dto.request;

import com.business.erp.employee.entity.Employee;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChangeEmployeeStatusRequest {
    @NotNull
    private Employee.EmployeeStatus targetStatus;

    @Size(max = 500)
    private String reason;

    private LocalDate noticeStartDate;
    private LocalDate lastWorkingDate;
}
