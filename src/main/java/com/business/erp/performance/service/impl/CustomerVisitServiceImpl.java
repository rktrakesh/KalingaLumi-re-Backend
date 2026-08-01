package com.business.erp.performance.service.impl;

import com.business.erp.customer.entity.Customer;
import com.business.erp.customer.service.CustomerService;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.performance.dto.request.LogCustomerVisitRequest;
import com.business.erp.performance.entity.CustomerVisit;
import com.business.erp.performance.repository.CustomerVisitRepository;
import com.business.erp.performance.service.CustomerVisitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerVisitServiceImpl implements CustomerVisitService {

    private final CustomerVisitRepository visitRepository;
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final Logger log = LoggerFactory.getLogger(CustomerVisitServiceImpl.class);

    @Override
    @Transactional
    public CustomerVisit logVisit(LogCustomerVisitRequest request, String actor) {
        Customer customer = customerService.getCustomer(request.getCustomerId());
        Employee visitedBy = employeeService.getEmployee(request.getVisitedByEmployeeId());

        CustomerVisit saved = visitRepository.save(CustomerVisit.builder()
                .customer(customer).visitedBy(visitedBy)
                .visitDate(request.getVisitDate())
                .visitPurpose(request.getVisitPurpose()).visitOutcome(request.getVisitOutcome())
                .remarks(request.getRemarks())
                .createdBy(actor).createdDate(LocalDateTime.now())
                .build());

        log.info("CustomerVisitServiceImpl:logVisit :: customerId={} visitedBy={} outcome={} by={}",
                request.getCustomerId(), request.getVisitedByEmployeeId(), request.getVisitOutcome(), actor);
        return saved;
    }

    @Override
    public List<CustomerVisit> getVisitHistory(Long customerId) {
        return visitRepository.findByCustomerIdOrderByVisitDateDesc(customerId);
    }
}