package com.business.erp.leave.repository;

import com.business.erp.leave.entity.LeaveSettlementLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveSettlementLogRepository extends JpaRepository<LeaveSettlementLog, Long> {
    Optional<LeaveSettlementLog> findByEmployeeIdAndYearAndMonth(Long employeeId, int year, int month);
}