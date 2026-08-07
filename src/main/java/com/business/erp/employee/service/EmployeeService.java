package com.business.erp.employee.service;

import com.business.erp.employee.dto.request.CreateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateSalaryRequest;
import com.business.erp.employee.dto.request.ChangeEmployeeStatusRequest;
import com.business.erp.employee.dto.response.EmployeeResponse;
import com.business.erp.employee.dto.response.SalaryHistoryResponse;
import com.business.erp.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface EmployeeService {
    EmployeeResponse create(CreateEmployeeRequest request, String createdBy);

    Page<EmployeeResponse> findAll(Employee.EmployeeStatus status, String search, Pageable pageable);

    EmployeeResponse findById(Long id);

    EmployeeResponse update(Long id, UpdateEmployeeRequest request);

    EmployeeResponse updateSalary(Long id, UpdateSalaryRequest request, String updatedBy);

    EmployeeResponse deactivate(Long id);

    EmployeeResponse deactivate(Long id, String actor);

    EmployeeResponse changeStatus(Long id, ChangeEmployeeStatusRequest request, String actor);

    List<SalaryHistoryResponse> getSalaryHistory(Long id);

    Employee getEmployee(Long id);

    Employee getOperationalEmployee(Long id);

    List<Employee> getAttendanceEligibleEmployees();

    /** Batch lookup by ID, keyed by employee ID — avoids N+1 single-row queries in callers like payroll reporting. */
    Map<Long, Employee> getEmployeesByIds(List<Long> ids);
}
