package com.business.erp.overtime.repository;

import com.business.erp.overtime.entity.OvertimeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface OvertimeRepository extends JpaRepository<OvertimeRequest, Long> {
    @Query("SELECT COUNT(o) FROM OvertimeRequest o WHERE o.overtimeDate BETWEEN :from AND :to AND o.status = :status")
    long countByMonthAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to,
                               @Param("status") OvertimeRequest.OvertimeStatus status);

    @Query("SELECT COALESCE(SUM(o.approvedMinutes), 0) FROM OvertimeRequest o WHERE o.employee.id = :empId " +
            "AND o.overtimeDate BETWEEN :from AND :to AND o.status IN ('APPROVED','MODIFIED')")
    Integer sumApprovedMinutesByEmployeeAndMonth(@Param("empId") Long empId,
                                                 @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT o FROM OvertimeRequest o WHERE o.employee.id = :empId AND o.overtimeDate BETWEEN :from AND :to " +
            "AND o.status IN ('APPROVED','MODIFIED')")
    java.util.List<OvertimeRequest> findApprovedInRange(@Param("empId") Long empId,
                                                        @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT o FROM OvertimeRequest o WHERE o.employee.id = :empId AND o.overtimeDate BETWEEN :from AND :to " +
            "AND o.status = 'PENDING'")
    java.util.List<OvertimeRequest> findPendingInRange(@Param("empId") Long empId,
                                                       @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT o FROM OvertimeRequest o WHERE (:empId IS NULL OR o.employee.id = :empId) AND " +
            "(:status IS NULL OR o.status = :status) ORDER BY o.overtimeDate DESC")
    Page<OvertimeRequest> search(@Param("empId") Long empId,
                                 @Param("status") OvertimeRequest.OvertimeStatus status, Pageable pageable);

    @Query("SELECT o FROM OvertimeRequest o WHERE o.attendance.id = :attendanceId AND o.requestType = 'EXCESS_HOURS' " +
            "AND o.status IN ('PENDING','APPROVED','MODIFIED')")
    java.util.Optional<OvertimeRequest> findActiveExcessHoursRequest(@Param("attendanceId") Long attendanceId);
}