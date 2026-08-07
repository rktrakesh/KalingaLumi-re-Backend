package com.business.erp.employee.service.impl;

import com.business.erp.auth.service.UserManagementService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.dto.request.ChangeEmployeeStatusRequest;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.enums.EmployeeDocumentStatus;
import com.business.erp.employee.enums.EmployeeDocumentType;
import com.business.erp.employee.repository.EmployeeDocumentRepository;
import com.business.erp.employee.repository.EmployeeRepository;
import com.business.erp.employee.service.EmployeeLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeLifecycleServiceImpl implements EmployeeLifecycleService {

    private static final Map<Employee.EmployeeStatus, Set<Employee.EmployeeStatus>> TRANSITIONS =
            approvedTransitions();

    private final EmployeeRepository employeeRepository;
    private final EmployeeDocumentRepository documentRepository;
    private final UserManagementService userManagementService;
    private final AuditService auditService;
    private final ClockProvider clockProvider;

    @Override
    public List<Employee.EmployeeStatus> allowedTransitions(Employee.EmployeeStatus currentStatus) {
        return TRANSITIONS.getOrDefault(currentStatus, Set.of()).stream().toList();
    }

    @Override
    @Transactional
    public Employee transition(Long employeeId, ChangeEmployeeStatusRequest request, String actor) {
        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        Employee.EmployeeStatus previous = employee.getStatus();
        LocalDate previousNoticeStartDate = employee.getNoticeStartDate();
        LocalDate previousLastWorkingDate = employee.getLastWorkingDate();
        Employee.EmployeeStatus target = request.getTargetStatus();
        if (!TRANSITIONS.getOrDefault(previous, Set.of()).contains(target)) {
            throw new BusinessException("Employee status cannot transition from " + previous + " to " + target);
        }

        String reason = normalizeReason(request.getReason());
        if (requiresReason(target) && reason == null) {
            throw new BusinessException("A lifecycle reason is required when changing status to " + target);
        }

        LocalDate noticeStartDate = request.getNoticeStartDate();
        LocalDate lastWorkingDate = request.getLastWorkingDate();
        if (target == Employee.EmployeeStatus.ON_NOTICE) {
            if (noticeStartDate == null || lastWorkingDate == null) {
                throw new BusinessException("Notice start date and last working date are required for ON_NOTICE");
            }
            validateDateOrder(noticeStartDate, lastWorkingDate);
            employee.setNoticeStartDate(noticeStartDate);
            employee.setLastWorkingDate(lastWorkingDate);
        } else if (target == Employee.EmployeeStatus.RESIGNED) {
            LocalDate finalLastWorkingDate = lastWorkingDate != null
                    ? lastWorkingDate : employee.getLastWorkingDate();
            if (finalLastWorkingDate == null) {
                throw new BusinessException("Last working date is required for RESIGNED");
            }
            if (finalLastWorkingDate.isAfter(clockProvider.today())) {
                throw new BusinessException("A RESIGNED employee cannot have a future last working date");
            }
            if (previous == Employee.EmployeeStatus.ACTIVE) {
                LocalDate immediateNoticeDate = noticeStartDate != null ? noticeStartDate : finalLastWorkingDate;
                if (!immediateNoticeDate.equals(finalLastWorkingDate)) {
                    throw new BusinessException("Direct ACTIVE to RESIGNED requires a zero-notice separation date");
                }
                employee.setNoticeStartDate(immediateNoticeDate);
            } else if (employee.getNoticeStartDate() == null && noticeStartDate != null) {
                employee.setNoticeStartDate(noticeStartDate);
            }
            validateDateOrder(employee.getNoticeStartDate(), finalLastWorkingDate);
            employee.setLastWorkingDate(finalLastWorkingDate);
        } else if (target == Employee.EmployeeStatus.ACTIVE) {
            if (previous == Employee.EmployeeStatus.DRAFT) {
                requireActivationDocuments(employeeId);
            }
            employee.setNoticeStartDate(null);
            employee.setLastWorkingDate(null);
        }

        employee.setStatus(target);
        Employee saved = employeeRepository.save(employee);

        if (target == Employee.EmployeeStatus.RESIGNED
                || target == Employee.EmployeeStatus.INACTIVE) {
            userManagementService.invalidateSessionsForEmployee(employeeId, actor,
                    "Employee lifecycle changed from " + previous + " to " + target);
        }

        Object changedAt = clockProvider.now();
        Map<String, Object> oldValue = lifecycleSnapshot(previous, null,
                previousNoticeStartDate, previousLastWorkingDate, changedAt);
        Map<String, Object> newValue = lifecycleSnapshot(target, reason,
                saved.getNoticeStartDate(), saved.getLastWorkingDate(), changedAt);
        auditService.logAs(actor, "EMPLOYEE", "STATUS_CHANGE", "Employee", employeeId,
                oldValue, newValue);
        return saved;
    }

    private void requireActivationDocuments(Long employeeId) {
        for (EmployeeDocumentType type : EmployeeDocumentType.values()) {
            if (type.isRequiredForActivation()
                    && !documentRepository.existsByEmployeeIdAndDocumentTypeAndStatus(
                    employeeId, type, EmployeeDocumentStatus.CURRENT)) {
                throw new BusinessException(type.name().replace('_', ' ')
                        + " is required before activating a draft employee");
            }
        }
    }

    private boolean requiresReason(Employee.EmployeeStatus target) {
        return target == Employee.EmployeeStatus.ON_NOTICE
                || target == Employee.EmployeeStatus.RESIGNED
                || target == Employee.EmployeeStatus.INACTIVE;
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private void validateDateOrder(LocalDate noticeStartDate, LocalDate lastWorkingDate) {
        if (noticeStartDate != null && lastWorkingDate.isBefore(noticeStartDate)) {
            throw new BusinessException("Last working date cannot be before notice start date");
        }
    }

    private Map<String, Object> lifecycleSnapshot(Employee.EmployeeStatus status, String reason,
                                                   LocalDate noticeStartDate, LocalDate lastWorkingDate,
                                                   Object timestamp) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", status);
        value.put("reason", reason);
        value.put("noticeStartDate", noticeStartDate);
        value.put("lastWorkingDate", lastWorkingDate);
        value.put("timestamp", timestamp);
        return value;
    }

    private static Map<Employee.EmployeeStatus, Set<Employee.EmployeeStatus>> approvedTransitions() {
        Map<Employee.EmployeeStatus, Set<Employee.EmployeeStatus>> transitions =
                new EnumMap<>(Employee.EmployeeStatus.class);
        transitions.put(Employee.EmployeeStatus.DRAFT,
                EnumSet.of(Employee.EmployeeStatus.ACTIVE, Employee.EmployeeStatus.INACTIVE));
        transitions.put(Employee.EmployeeStatus.ACTIVE,
                EnumSet.of(Employee.EmployeeStatus.ON_NOTICE, Employee.EmployeeStatus.INACTIVE,
                        Employee.EmployeeStatus.RESIGNED));
        transitions.put(Employee.EmployeeStatus.ON_NOTICE,
                EnumSet.of(Employee.EmployeeStatus.ACTIVE, Employee.EmployeeStatus.RESIGNED,
                        Employee.EmployeeStatus.INACTIVE));
        transitions.put(Employee.EmployeeStatus.INACTIVE, EnumSet.of(Employee.EmployeeStatus.ACTIVE));
        transitions.put(Employee.EmployeeStatus.RESIGNED, EnumSet.noneOf(Employee.EmployeeStatus.class));
        return transitions;
    }
}
