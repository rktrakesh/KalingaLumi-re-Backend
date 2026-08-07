package com.business.erp.performance.service.impl;

import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.customer.entity.Customer;
import com.business.erp.customer.service.CustomerService;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.enums.EmployeeCategoryCode;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.performance.entity.CustomerOwnership;
import com.business.erp.performance.enums.OwnershipStatus;
import com.business.erp.performance.repository.CustomerOwnershipRepository;
import com.business.erp.performance.service.CustomerOwnershipService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerOwnershipServiceImpl implements CustomerOwnershipService {

    private final CustomerOwnershipRepository ownershipRepository;
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final ClockProvider clockProvider;
    private final Logger log = LoggerFactory.getLogger(CustomerOwnershipServiceImpl.class);

    @Override
    @Transactional
    public CustomerOwnership assignOwner(Long customerId, Long employeeId, LocalDate effectiveFrom, String remarks, String actor) {
        Customer customer = customerService.getCustomer(customerId);
        Employee employee = requireSalesEmployee(employeeId);

        Optional<CustomerOwnership> currentPermanent = ownershipRepository
                .findByCustomerIdAndStatusAndIsTemporary(customerId, OwnershipStatus.ACTIVE, false);

        if (currentPermanent.isPresent() && currentPermanent.get().getEmployee().getId().equals(employeeId)) {
            log.info("CustomerOwnershipServiceImpl:assignOwner :: customerId={} already owned by employeeId={} — no-op",
                    customerId, employeeId);
            return currentPermanent.get();
        }

        currentPermanent.ifPresent(previous -> {
            previous.setStatus(OwnershipStatus.TRANSFERRED);
            previous.setEffectiveTo(effectiveFrom.minusDays(1));
            ownershipRepository.save(previous);
            log.info("CustomerOwnershipServiceImpl:assignOwner :: superseded previous ownerId={} for customerId={}",
                    previous.getEmployee().getId(), customerId);
        });

        CustomerOwnership created = ownershipRepository.save(CustomerOwnership.builder()
                .customer(customer).employee(employee)
                .effectiveFrom(effectiveFrom).effectiveTo(null)
                .isTemporary(false).status(OwnershipStatus.ACTIVE)
                .remarks(remarks).build());

        log.info("CustomerOwnershipServiceImpl:assignOwner :: customerId={} now owned by employeeId={} from={} by={}",
                customerId, employeeId, effectiveFrom, actor);
        return created;
    }

    @Override
    @Transactional
    public List<CustomerOwnership> transferCustomers(List<Long> customerIds, Long toEmployeeId, LocalDate effectiveFrom, String remarks, String actor) {
        List<CustomerOwnership> results = new ArrayList<>(customerIds.size());
        for (Long customerId : customerIds) {
            results.add(assignOwner(customerId, toEmployeeId, effectiveFrom, remarks, actor));
        }
        log.info("CustomerOwnershipServiceImpl:transferCustomers :: transferred {} customers to employeeId={} by={}",
                results.size(), toEmployeeId, actor);
        return results;
    }

    @Override
    @Transactional
    public CustomerOwnership assignTemporary(Long customerId, Long employeeId, LocalDate effectiveFrom, LocalDate effectiveTo, String remarks, String actor) {
        if (effectiveTo == null || !effectiveTo.isAfter(effectiveFrom)) {
            throw new BusinessException("INVALID_RANGE: temporary assignment's effectiveTo must be after effectiveFrom");
        }
        Customer customer = customerService.getCustomer(customerId);
        Employee employee = requireSalesEmployee(employeeId);

        boolean overlaps = ownershipRepository.findByCustomerIdOrderByEffectiveFromDesc(customerId).stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsTemporary()) && o.getStatus() == OwnershipStatus.ACTIVE)
                .anyMatch(o -> !(effectiveTo.isBefore(o.getEffectiveFrom())
                        || (o.getEffectiveTo() != null && effectiveFrom.isAfter(o.getEffectiveTo()))));
        if (overlaps) {
            throw new BusinessException("OVERLAPPING_TEMPORARY_ASSIGNMENT: customer already has a temporary assignment covering part of this range");
        }

        CustomerOwnership created = ownershipRepository.save(CustomerOwnership.builder()
                .customer(customer).employee(employee)
                .effectiveFrom(effectiveFrom).effectiveTo(effectiveTo)
                .isTemporary(true).status(OwnershipStatus.ACTIVE)
                .remarks(remarks).build());

        log.info("CustomerOwnershipServiceImpl:assignTemporary :: customerId={} temporarily to employeeId={} {}..{} by={}",
                customerId, employeeId, effectiveFrom, effectiveTo, actor);
        return created;
    }

    @Override
    @Transactional
    public CustomerOwnership makeTemporaryPermanent(Long temporaryOwnershipId, String actor) {
        CustomerOwnership temp = ownershipRepository.findById(temporaryOwnershipId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerOwnership", temporaryOwnershipId));
        if (!Boolean.TRUE.equals(temp.getIsTemporary())) {
            throw new BusinessException("NOT_TEMPORARY: ownership record " + temporaryOwnershipId + " is not a temporary assignment");
        }

        CustomerOwnership permanent = assignOwner(temp.getCustomer().getId(), temp.getEmployee().getId(),
                temp.getEffectiveFrom(), temp.getRemarks(), actor);

        temp.setStatus(OwnershipStatus.TRANSFERRED);
        temp.setEffectiveTo(clockProvider.today());
        ownershipRepository.save(temp);

        log.info("CustomerOwnershipServiceImpl:makeTemporaryPermanent :: temporaryOwnershipId={} promoted by={}",
                temporaryOwnershipId, actor);
        return permanent;
    }

    @Override
    public Optional<Employee> resolveCurrentOwner(Long customerId) {
        return resolveOwnerAsOf(customerId, clockProvider.today());
    }

    @Override
    public Optional<Employee> resolveOwnerAsOf(Long customerId, LocalDate asOf) {
        List<CustomerOwnership> candidates = ownershipRepository.findByCustomerIdOrderByEffectiveFromDesc(customerId).stream()
                .filter(o -> o.getStatus() != OwnershipStatus.TRANSFERRED)
                .filter(o -> !o.getEffectiveFrom().isAfter(asOf))
                .filter(o -> o.getEffectiveTo() == null || !o.getEffectiveTo().isBefore(asOf))
                .toList();

        // A temporary assignment covering this date always takes precedence over the
        // permanent owner — that's the entire point of a temporary reassignment.
        return candidates.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsTemporary()))
                .findFirst()
                .or(() -> candidates.stream().filter(o -> !Boolean.TRUE.equals(o.getIsTemporary())).findFirst())
                .map(CustomerOwnership::getEmployee);
    }

    @Override
    public List<CustomerOwnership> getOwnershipHistory(Long customerId) {
        return ownershipRepository.findByCustomerIdOrderByEffectiveFromDesc(customerId);
    }

    @Override
    public List<CustomerOwnership> getAssignedCustomers(Long employeeId) {
        LocalDate today = clockProvider.today();
        return ownershipRepository.findActiveOwnershipsForEmployee(employeeId).stream()
                .filter(o -> !o.getEffectiveFrom().isAfter(today))
                .filter(o -> o.getEffectiveTo() == null || !o.getEffectiveTo().isBefore(today))
                .toList();
    }

    private Employee requireSalesEmployee(Long employeeId) {
        Employee employee = employeeService.getEmployee(employeeId);
        if (!EmployeeCategoryCode.SALES.equals(employee.getEmployeeCategory().getCode())) {
            throw new BusinessException("NOT_SALES_EMPLOYEE: only SALES-category employees can own customers " +
                    "(employeeId=" + employeeId + " is " + employee.getEmployeeCategory().getCode() + ")");
        }
        return employee;
    }
}