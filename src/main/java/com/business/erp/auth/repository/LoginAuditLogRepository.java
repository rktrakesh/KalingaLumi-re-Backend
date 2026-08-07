package com.business.erp.auth.repository;

import com.business.erp.auth.entity.LoginAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, Long> {
    List<LoginAuditLog> findByUserIdOrderByEventDateDesc(Long userId);
}