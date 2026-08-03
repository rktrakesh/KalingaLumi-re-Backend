package com.business.erp.employee.service.impl;

import com.business.erp.auth.service.UserOnboardingService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.employee.dto.request.CreateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateSalaryRequest;
import com.business.erp.employee.dto.response.EmployeeResponse;
import com.business.erp.employee.dto.response.SalaryHistoryResponse;
import com.business.erp.employee.entity.DepartmentMaster;
import com.business.erp.employee.entity.DesignationMaster;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.entity.EmployeeCategoryMaster;
import com.business.erp.employee.entity.EmployeeSalaryHistory;
import com.business.erp.employee.repository.EmployeeRepository;
import com.business.erp.employee.repository.EmployeeSalaryHistoryRepository;
import com.business.erp.employee.service.DepartmentMasterService;
import com.business.erp.employee.service.DesignationMasterService;
import com.business.erp.employee.service.EmployeeCategoryMasterService;
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
    private final EmployeeCategoryMasterService categoryMasterService;
    private final DesignationMasterService designationMasterService;
    private final DepartmentMasterService departmentMasterService;
    private final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req, String createdBy) {
        log.info("EmployeeServiceImpl:create :: Creating employee name={} by={}", req.getName(), createdBy);
        String code = refService.generateEmployeeCode();

        EmployeeCategoryMaster category = categoryMasterService.getEntityById(req.getEmployeeCategoryId());
        DesignationMaster designation = designationMasterService.getEntityById(req.getDesignationId());
        DepartmentMaster department = req.getDepartmentId() != null
                ? departmentMasterService.getEntityById(req.getDepartmentId()) : null;
        Employee reportingManager = req.getReportingManagerId() != null
                ? getEmployee(req.getReportingManagerId()) : null;

        Employee emp = Employee.builder()
                .employeeCode(code).name(req.getName()).phone(req.getPhone())
                .address(req.getAddress()).email(req.getEmail()).joiningDate(req.getJoiningDate())
                .designation(designation).department(department).employeeCategory(category)
                .employmentType(req.getEmploymentType()).reportingManager(reportingManager)
                .dateOfBirth(req.getDateOfBirth()).gender(req.getGender())
                .emergencyContactName(req.getEmergencyContactName())
                .emergencyContactPhone(req.getEmergencyContactPhone())
                .currentSalary(req.getCurrentSalary())
                .panNumber(req.getPanNumber()).bankAccountNumber(req.getBankAccountNumber())
                .bankIfsc(req.getBankIfsc()).bankName(req.getBankName())
                .bankAccountHolderName(req.getBankAccountHolderName())
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
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> findAll(Employee.EmployeeStatus status, String search, Pageable pageable) {
        log.debug("EmployeeServiceImpl:findAll :: status={} search={}", status, search);
        return employeeRepository.findWithFilters(status, search, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
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
        if (req.getEmail() != null) emp.setEmail(req.getEmail());
        if (req.getDesignationId() != null) emp.setDesignation(designationMasterService.getEntityById(req.getDesignationId()));
        if (req.getDepartmentId() != null) emp.setDepartment(departmentMasterService.getEntityById(req.getDepartmentId()));
        if (req.getEmployeeCategoryId() != null) emp.setEmployeeCategory(categoryMasterService.getEntityById(req.getEmployeeCategoryId()));
        if (req.getEmploymentType() != null) emp.setEmploymentType(req.getEmploymentType());
        if (req.getReportingManagerId() != null) emp.setReportingManager(getEmployee(req.getReportingManagerId()));
        if (req.getDateOfBirth() != null) emp.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null) emp.setGender(req.getGender());
        if (req.getEmergencyContactName() != null) emp.setEmergencyContactName(req.getEmergencyContactName());
        if (req.getEmergencyContactPhone() != null) emp.setEmergencyContactPhone(req.getEmergencyContactPhone());
        if (req.getPanNumber() != null) emp.setPanNumber(req.getPanNumber());
        if (req.getBankAccountNumber() != null) emp.setBankAccountNumber(req.getBankAccountNumber());
        if (req.getBankIfsc() != null) emp.setBankIfsc(req.getBankIfsc());
        if (req.getBankName() != null) emp.setBankName(req.getBankName());
        if (req.getBankAccountHolderName() != null) emp.setBankAccountHolderName(req.getBankAccountHolderName());
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
        return employeeRepository.findByStatusIn(
                List.of(Employee.EmployeeStatus.ACTIVE, Employee.EmployeeStatus.ON_NOTICE));
    }

    @Override
    public java.util.Map<Long, Employee> getEmployeesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Map.of();
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
    }

    private EmployeeResponse toResponse(Employee e) {
        DesignationMaster designation = e.getDesignation();
        DepartmentMaster department = e.getDepartment();
        EmployeeCategoryMaster category = e.getEmployeeCategory();
        Employee manager = e.getReportingManager();

        return EmployeeResponse.builder()
                .id(e.getId()).employeeCode(e.getEmployeeCode()).status(e.getStatus().name())
                .name(e.getName()).phone(e.getPhone()).email(e.getEmail()).address(e.getAddress())
                .dateOfBirth(e.getDateOfBirth()).gender(e.getGender() != null ? e.getGender().name() : null)
                .emergencyContactName(e.getEmergencyContactName()).emergencyContactPhone(e.getEmergencyContactPhone())
                .joiningDate(e.getJoiningDate())
                .designationId(designation != null ? designation.getId() : null)
                .designationCode(designation != null ? designation.getCode() : null)
                .designationName(designation != null ? designation.getName() : null)
                .departmentId(department != null ? department.getId() : null)
                .departmentCode(department != null ? department.getCode() : null)
                .departmentName(department != null ? department.getName() : null)
                .employeeCategoryId(category != null ? category.getId() : null)
                .employeeCategoryCode(category != null ? category.getCode() : null)
                .employeeCategoryName(category != null ? category.getName() : null)
                .employmentType(e.getEmploymentType() != null ? e.getEmploymentType().name() : null)
                .reportingManagerId(manager != null ? manager.getId() : null)
                .reportingManagerName(manager != null ? manager.getName() : null)
                .currentSalary(e.getCurrentSalary())
                .panNumber(e.getPanNumber()).bankAccountNumber(e.getBankAccountNumber())
                .bankIfsc(e.getBankIfsc()).bankName(e.getBankName())
                .bankAccountHolderName(e.getBankAccountHolderName())
                .createdBy(e.getCreatedBy()).createdDate(e.getCreatedDate())
                .build();
    }
}