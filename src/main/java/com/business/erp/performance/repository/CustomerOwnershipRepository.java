package com.business.erp.performance.repository;

import com.business.erp.performance.entity.CustomerOwnership;
import com.business.erp.performance.enums.OwnershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerOwnershipRepository extends JpaRepository<CustomerOwnership, Long> {

    Optional<CustomerOwnership> findByCustomerIdAndStatus(Long customerId, OwnershipStatus status);

    Optional<CustomerOwnership> findByCustomerIdAndStatusAndIsTemporary(Long customerId, OwnershipStatus status, Boolean isTemporary);

    List<CustomerOwnership> findByEmployeeIdAndStatus(Long employeeId, OwnershipStatus status);

    @Query("SELECT co FROM CustomerOwnership co WHERE co.employee.id = :employeeId AND co.status = 'ACTIVE'")
    List<CustomerOwnership> findActiveOwnershipsForEmployee(@Param("employeeId") Long employeeId);

    List<CustomerOwnership> findByCustomerIdOrderByEffectiveFromDesc(Long customerId);
}