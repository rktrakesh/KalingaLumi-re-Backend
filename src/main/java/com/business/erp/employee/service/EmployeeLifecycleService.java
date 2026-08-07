package com.business.erp.employee.service;

import com.business.erp.employee.dto.request.ChangeEmployeeStatusRequest;
import com.business.erp.employee.entity.Employee;

public interface EmployeeLifecycleService {
    Employee transition(Long employeeId, ChangeEmployeeStatusRequest request, String actor);

    java.util.List<Employee.EmployeeStatus> allowedTransitions(Employee.EmployeeStatus currentStatus);
}
