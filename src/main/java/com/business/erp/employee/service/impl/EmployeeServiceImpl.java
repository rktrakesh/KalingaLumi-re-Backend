package com.business.erp.employee.service.impl;

import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.auth.service.UserOnboardingService;
import com.business.erp.employee.dto.request.CreateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateSalaryRequest;
import com.business.erp.employee.dto.response.EmployeeResponse;
import com.business.erp.employee.dto.response.SalaryHistoryResponse;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.entity.EmployeeSalaryHistory;
import com.business.erp.employee.enums.EmployeeCategory;
import com.business.erp.employee.repository.EmployeeRepository;
import com.business.erp.employee.repository.EmployeeSalaryHistoryRepository;
import com.business.erp.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryHistoryRepository salaryHistoryRepository;
    private final ReferenceNumberService refService;
    private final AuditService auditService;
    private final UserOnboardingService userOnboardingService;
    private final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req, String createdBy) {
        log.info("EmployeeServiceImpl:create :: Creating employee name={} by={}", req.getName(), createdBy);
        String code = refService.generateEmployeeCode();
        Employee emp = Employee.builder()
                .employeeCode(code).name(req.getName()).phone(req.getPhone())
                .address(req.getAddress()).email(req.getEmail()).joiningDate(req.getJoiningDate())
                .designation(req.getDesignation())
                .employeeCategory(req.getEmployeeCategory() != null ? req.getEmployeeCategory() : EmployeeCategory.FACTORY)
                .currentSalary(req.getCurrentSalary())
                .status(Employee.EmployeeStatus.ACTIVE).build();
        emp = employeeRepository.save(emp);
        salaryHistoryRepository.save(EmployeeSalaryHistory.builder()
                .employee(emp).salary(req.getCurrentSalary()).effectiveFrom(req.getJoiningDate())
                .remarks(req.getSalaryRemarks() != null ? req.getSalaryRemarks() : "Initial salary")
                .createdBy(createdBy).build());
        auditService.log("EMPLOYEE", "CREATE", "Employee", emp.getId());

        userOnboardingService.onboard(emp, createdBy);

        log.info("EmployeeServiceImpl:create :: SUCCESS code={} id={}", code, emp.getId());
        return toResponse(emp);
    }

    @Override
    public Page<EmployeeResponse> findAll(Employee.EmployeeStatus status, String search, Pageable pageable) {
        log.debug("EmployeeServiceImpl:findAll :: status={} search={}", status, search);
        return employeeRepository.findWithFilters(status, search, pageable).map(this::toResponse);
    }

    @Override
    public EmployeeResponse findById(Long id) {
        log.debug("EmployeeServiceImpl:findById :: id={}", id);
        return toResponse(getEmployee(id));
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest req) {
        log.info("EmployeeServiceImpl:update :: id={}", id);
        Employee emp = getEmployee(id);
        if (req.getName() != null) emp.setName(req.getName());
        if (req.getPhone() != null) emp.setPhone(req.getPhone());
        if (req.getAddress() != null) emp.setAddress(req.getAddress());
        if (req.getDesignation() != null) emp.setDesignation(req.getDesignation());
        return toResponse(employeeRepository.save(emp));
    }

    @Override
    @Transactional
    public EmployeeResponse updateSalary(Long id, UpdateSalaryRequest req, String updatedBy) {
        log.info("EmployeeServiceImpl:updateSalary :: id={} newSalary={} by={}", id, req.getNewSalary(), updatedBy);
        Employee emp = getEmployee(id);
        Object oldVal = emp.getCurrentSalary();
        emp.setCurrentSalary(req.getNewSalary());
        employeeRepository.save(emp);
        salaryHistoryRepository.save(EmployeeSalaryHistory.builder()
                .employee(emp).salary(req.getNewSalary()).effectiveFrom(req.getEffectiveFrom())
                .remarks(req.getRemarks()).createdBy(updatedBy).build());
        auditService.log("EMPLOYEE", "SALARY_CHANGE", "Employee", id, oldVal, req.getNewSalary());
        log.info("EmployeeServiceImpl:updateSalary :: SUCCESS id={}", id);
        return toResponse(emp);
    }

    @Override
    @Transactional
    public EmployeeResponse deactivate(Long id) {
        log.info("EmployeeServiceImpl:deactivate :: id={}", id);
        Employee emp = getEmployee(id);
        emp.setStatus(Employee.EmployeeStatus.INACTIVE);
        auditService.log("EMPLOYEE", "DEACTIVATE", "Employee", id);
        return toResponse(employeeRepository.save(emp));
    }

    @Override
    public List<SalaryHistoryResponse> getSalaryHistory(Long id) {
        log.debug("EmployeeServiceImpl:getSalaryHistory :: id={}", id);
        getEmployee(id);
        return salaryHistoryRepository.findByEmployeeIdOrderByEffectiveFromDesc(id).stream()
                .map(h -> SalaryHistoryResponse.builder().id(h.getId()).salary(h.getSalary())
                        .effectiveFrom(h.getEffectiveFrom()).remarks(h.getRemarks())
                        .createdBy(h.getCreatedBy()).createdDate(h.getCreatedDate()).build())
                .collect(Collectors.toList());
    }

    @Override
    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("EmployeeServiceImpl:getEmployee :: NOT FOUND id={}", id);
                    return new ResourceNotFoundException("Employee", id);
                });
    }

    @Override
    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);
    }

    @Override
    public java.util.Map<Long, Employee> getEmployeesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Map.of();
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
    }

    private EmployeeResponse toResponse(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId()).employeeCode(e.getEmployeeCode()).name(e.getName())
                .phone(e.getPhone()).address(e.getAddress()).joiningDate(e.getJoiningDate())
                .designation(e.getDesignation()).currentSalary(e.getCurrentSalary())
                .status(e.getStatus().name()).createdBy(e.getCreatedBy()).createdDate(e.getCreatedDate()).build();
    }
}