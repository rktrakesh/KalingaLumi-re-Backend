package com.business.erp.employee.repository;

import com.business.erp.employee.entity.EmployeeDocument;
import com.business.erp.employee.enums.EmployeeDocumentStatus;
import com.business.erp.employee.enums.EmployeeDocumentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {

    List<EmployeeDocument> findByEmployeeIdAndStatusOrderByDocumentTypeAscCreatedDateDesc(
            Long employeeId, EmployeeDocumentStatus status);

    boolean existsByEmployeeIdAndDocumentTypeAndStatus(
            Long employeeId, EmployeeDocumentType type, EmployeeDocumentStatus status);

    Optional<EmployeeDocument> findFirstByEmployeeIdAndDocumentTypeAndStatusOrderByCreatedDateDesc(
            Long employeeId, EmployeeDocumentType type, EmployeeDocumentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM EmployeeDocument d WHERE d.employee.id = :employeeId " +
            "AND d.documentType = :type AND d.status = :status ORDER BY d.createdDate DESC")
    List<EmployeeDocument> findCurrentForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("type") EmployeeDocumentType type,
            @Param("status") EmployeeDocumentStatus status);

    @Query("SELECT d FROM EmployeeDocument d JOIN FETCH d.employee WHERE d.id = :id")
    Optional<EmployeeDocument> findByIdWithEmployee(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM EmployeeDocument d JOIN FETCH d.employee WHERE d.id = :id")
    Optional<EmployeeDocument> findByIdForUpdate(@Param("id") Long id);
}
