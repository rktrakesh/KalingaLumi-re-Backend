package com.business.erp.employee.service.impl;

import com.business.erp.auth.service.UserOnboardingService;
import com.business.erp.auth.service.LoginIdentifierService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.employee.dto.request.CreateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateSalaryRequest;
import com.business.erp.employee.dto.request.ChangeEmployeeStatusRequest;
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
import com.business.erp.employee.service.EmployeeLifecycleService;
import com.business.erp.employee.validation.DesignationCategoryValidator;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.IdentifierTemplateRenderer;
import com.business.erp.settings.service.IdentifierTemplateContext;
import com.business.erp.settings.service.SettingsService;
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
    private final LoginIdentifierService loginIdentifierService;
    private final EmployeeCategoryMasterService categoryMasterService;
    private final DesignationMasterService designationMasterService;
    private final DepartmentMasterService departmentMasterService;
    private final DesignationCategoryValidator designationCategoryValidator;
    private final SettingsService settingsService;
    private final EmployeeLifecycleService employeeLifecycleService;
    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req, String createdBy) {
        log.info("EmployeeServiceImpl:create :: Creating employee name={} by={}", req.getName(), createdBy);
        long joiningSequence = refService.nextEmployeeNumber();
        IdentifierTemplateContext identifierContext = new IdentifierTemplateContext(
                settingsService.getCurrentValue(SettingKey.COMPANY_NAME),
                settingsService.getCurrentValue(SettingKey.COMPANY_SHORT_NAME),
                req.getName(), joiningSequence, req.getJoiningDate());
        String code = loginIdentifierService.normalizeEmployeeCode(
                IdentifierTemplateRenderer.renderEmployeeCode(
                        settingsService.getCurrentValue(SettingKey.EMPLOYEE_CODE_TEMPLATE),
                        identifierContext));
        loginIdentifierService.validateEmployeeCodeAvailable(code, null);

        EmployeeCategoryMaster category = categoryMasterService.getEntityById(req.getEmployeeCategoryId());
        DesignationMaster designation = designationMasterService.getEntityById(req.getDesignationId());
        designationCategoryValidator.validate(designation, category);
        DepartmentMaster department = req.getDepartmentId() != null
                ? departmentMasterService.getEntityById(req.getDepartmentId()) : null;
        Employee reportingManager = req.getReportingManagerId() != null
                ? getEmployee(req.getReportingManagerId()) : null;

        Employee.EmployeeStatus initialStatus = req.getStatus() == null
                ? Employee.EmployeeStatus.ACTIVE : req.getStatus();
        if (initialStatus != Employee.EmployeeStatus.ACTIVE
                && initialStatus != Employee.EmployeeStatus.DRAFT) {
            throw new BusinessException("A new employee can only be created as DRAFT or ACTIVE");
        }

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
                .status(initialStatus).build();
        emp = employeeRepository.save(emp);
        salaryHistoryRepository.save(EmployeeSalaryHistory.builder()
                .employee(emp).salary(req.getCurrentSalary()).effectiveFrom(req.getJoiningDate())
                .remarks(req.getSalaryRemarks() != null ? req.getSalaryRemarks() : "Initial salary")
                .createdBy(createdBy).build());
        auditService.log("EMPLOYEE", "CREATE", "Employee", emp.getId());

        // User Onboarding Service (IAM): Employee Created -> Create User -> Default Role ->
        // Temporary Password -> Welcome Email -> mustChangePassword = true. One employee,
        // one user account, enforced by the unique+FK constraint on users.employee_id.
        if (req.getCreateLogin() == null || Boolean.TRUE.equals(req.getCreateLogin())) {
            userOnboardingService.onboard(emp, joiningSequence, createdBy);
        }

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

        // Designation and Category may each be updated independently or together — whichever
        // combination results, the designation must belong to the resulting category.
        if (req.getDesignationId() != null || req.getEmployeeCategoryId() != null) {
            DesignationMaster newDesignation = req.getDesignationId() != null
                    ? designationMasterService.getEntityById(req.getDesignationId()) : emp.getDesignation();
            EmployeeCategoryMaster newCategory = req.getEmployeeCategoryId() != null
                    ? categoryMasterService.getEntityById(req.getEmployeeCategoryId()) : emp.getEmployeeCategory();
            designationCategoryValidator.validate(newDesignation, newCategory);
            emp.setDesignation(newDesignation);
            emp.setEmployeeCategory(newCategory);
        }

        if (req.getDepartmentId() != null)
            emp.setDepartment(departmentMasterService.getEntityById(req.getDepartmentId()));
        if (req.getEmploymentType() != null) emp.setEmploymentType(req.getEmploymentType());
        if (req.getReportingManagerId() != null) {
            Employee newManager = getEmployee(req.getReportingManagerId());
            validateReportingManager(emp, newManager);
            emp.setReportingManager(newManager);
        }
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
        return deactivate(id, "SYSTEM");
    }

    @Override
    public EmployeeResponse deactivate(Long id, String actor) {
        ChangeEmployeeStatusRequest request = new ChangeEmployeeStatusRequest();
        request.setTargetStatus(Employee.EmployeeStatus.INACTIVE);
        request.setReason("Deactivated through compatibility endpoint");
        return changeStatus(id, request, actor);
    }

    @Override
    public EmployeeResponse changeStatus(Long id, ChangeEmployeeStatusRequest request, String actor) {
        return toResponse(employeeLifecycleService.transition(id, request, actor));
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
    public Employee getOperationalEmployee(Long id) {
        Employee employee = getEmployee(id);
        if (employee.getStatus() != Employee.EmployeeStatus.ACTIVE
                && employee.getStatus() != Employee.EmployeeStatus.ON_NOTICE) {
            throw new BusinessException("Employee status " + employee.getStatus()
                    + " is not eligible for this operation");
        }
        return employee;
    }

    @Override
    public List<Employee> getAttendanceEligibleEmployees() {
        // Payroll/Attendance/Login eligibility: ACTIVE and ON_NOTICE only (see Javadoc on
        // the interface method and Employee.EmployeeStatus).
        return employeeRepository.findByStatusIn(
                List.of(Employee.EmployeeStatus.ACTIVE, Employee.EmployeeStatus.ON_NOTICE));
    }

    @Override
    public java.util.Map<Long, Employee> getEmployeesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Map.of();
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
    }

    private void validateReportingManager(Employee emp, Employee newManager) {
        if (emp.getId() != null && emp.getId().equals(newManager.getId())) {
            throw new BusinessException("INVALID_REPORTING_MANAGER: an employee cannot be their own reporting manager");
        }
        java.util.Set<Long> visited = new java.util.HashSet<>();
        Employee current = newManager;
        while (current != null && current.getReportingManager() != null) {
            Long nextId = current.getReportingManager().getId();
            if (nextId.equals(emp.getId()) || !visited.add(nextId)) {
                throw new BusinessException("INVALID_REPORTING_MANAGER: this assignment would create a circular reporting chain");
            }
            current = current.getReportingManager();
        }
    }

    private EmployeeResponse toResponse(Employee e) {
        DesignationMaster designation = e.getDesignation();
        DepartmentMaster department = e.getDepartment();
        EmployeeCategoryMaster category = e.getEmployeeCategory();
        Employee manager = e.getReportingManager();

        return EmployeeResponse.builder()
                .id(e.getId()).employeeCode(e.getEmployeeCode()).status(e.getStatus().name())
                .allowedNextStatuses(employeeLifecycleService.allowedTransitions(e.getStatus()))
                .name(e.getName()).phone(e.getPhone()).email(e.getEmail()).address(e.getAddress())
                .dateOfBirth(e.getDateOfBirth()).gender(e.getGender() != null ? e.getGender().name() : null)
                .emergencyContactName(e.getEmergencyContactName()).emergencyContactPhone(e.getEmergencyContactPhone())
                .joiningDate(e.getJoiningDate())
                .noticeStartDate(e.getNoticeStartDate()).lastWorkingDate(e.getLastWorkingDate())
                .designation(designation != null ? designation.getName() : null)
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
