package com.business.erp.performance.service;

import com.business.erp.employee.entity.Employee;
import com.business.erp.performance.entity.CustomerOwnership;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerOwnershipService {

    CustomerOwnership assignOwner(Long customerId, Long employeeId, LocalDate effectiveFrom, String remarks, String actor);

    List<CustomerOwnership> transferCustomers(List<Long> customerIds, Long toEmployeeId, LocalDate effectiveFrom, String remarks, String actor);

    CustomerOwnership assignTemporary(Long customerId, Long employeeId, LocalDate effectiveFrom, LocalDate effectiveTo, String remarks, String actor);

    CustomerOwnership makeTemporaryPermanent(Long temporaryOwnershipId, String actor);

    Optional<Employee> resolveCurrentOwner(Long customerId);

    Optional<Employee> resolveOwnerAsOf(Long customerId, LocalDate asOf);

    List<CustomerOwnership> getOwnershipHistory(Long customerId);

    List<CustomerOwnership> getAssignedCustomers(Long employeeId);
}