package com.business.erp.audit.controller;

import com.business.erp.common.audit.AuditLog;
import com.business.erp.common.audit.AuditLogRepository;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Audit Logs", description = "Immutable audit trail — view all sensitive changes with old/new values (Admin only)")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final Logger log = LoggerFactory.getLogger(AuditController.class);

    @GetMapping
    @Operation(summary = "Search audit logs", description = "Filter by module, username, and date range")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> search(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("AuditController:search :: module={} username={}", module, username);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.of(auditLogRepository.search(module, username, from, to, pageable))));
    }

    @GetMapping("/{entityType}/{entityId}")
    @Operation(summary = "Get entity audit trail", description = "View all audit log entries for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getForEntity(
            @PathVariable String entityType, @PathVariable Long entityId) {
        log.debug("AuditController:getForEntity :: entityType={} entityId={}", entityType, entityId);
        return ResponseEntity.ok(ApiResponse.ok(
                auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)));
    }
}
