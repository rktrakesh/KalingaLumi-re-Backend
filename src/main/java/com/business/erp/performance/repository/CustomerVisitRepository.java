package com.business.erp.performance.repository;

import com.business.erp.performance.entity.CustomerVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CustomerVisitRepository extends JpaRepository<CustomerVisit, Long> {

    List<CustomerVisit> findByCustomerIdOrderByVisitDateDesc(Long customerId);

//    List<CustomerVisit> findByVisitedByEmployeeIdAndVisitDateBetween(Long employeeId, LocalDate from, LocalDate to);
}