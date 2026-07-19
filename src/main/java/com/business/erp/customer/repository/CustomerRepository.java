package com.business.erp.customer.repository;

import com.business.erp.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("SELECT c FROM Customer c WHERE (:status IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Customer> search(@Param("status") Customer.CustomerStatus status,
                          @Param("search") String search, Pageable pageable);

    List<Customer> findByStatus(Customer.CustomerStatus status);
}
