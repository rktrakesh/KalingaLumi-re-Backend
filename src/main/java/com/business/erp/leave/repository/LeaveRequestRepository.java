package com.business.erp.leave.repository;

import com.business.erp.leave.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedDateDesc(Long empId);

    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);

    @Query("SELECT l FROM LeaveRequest l WHERE " +
            "(:empId IS NULL OR l.employee.id = :empId) AND " +
            "(:status IS NULL OR l.status = :status) " +
            "ORDER BY l.leaveDate DESC")
    Page<LeaveRequest> search(@Param("empId") Long empId,
                              @Param("status") LeaveRequest.LeaveStatus status,
                              Pageable pageable);

    boolean existsByEmployeeIdAndLeaveDateAndStatusIn(Long empId, LocalDate date, List<LeaveRequest.LeaveStatus> statuses);
}
