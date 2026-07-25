package com.business.erp.leave.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable record of how one employee's unused paid leave for one month was settled
 * (expired / encashed / carried forward) under the UNUSED_PAID_LEAVE_POLICY in effect
 * for that payroll run's settings snapshot. Created once, when the payroll run for the
 * month reaches APPROVED.
 */
@Entity
@Table(name = "leave_settlement_logs",
        uniqueConstraints = @UniqueConstraint(name = "uq_leave_settlement_emp_month",
                columnNames = {"employee_id", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveSettlementLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_run_id", nullable = false)
    private Long payrollRunId;
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    @Column(nullable = false)
    private Integer year;
    @Column(nullable = false)
    private Integer month;

    @Column(name = "unused_days", nullable = false)
    private Integer unusedDays;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "policy_applied", nullable = false, length = 30)
    private com.business.erp.settings.enums.UnusedLeavePolicy policyApplied;

    @Column(name = "expired_days", nullable = false)
    @Builder.Default
    private Integer expiredDays = 0;
    @Column(name = "encashed_days", nullable = false)
    @Builder.Default
    private Integer encashedDays = 0;
    @Column(name = "encashment_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal encashmentAmount = BigDecimal.ZERO;
    @Column(name = "carried_forward_days", nullable = false)
    @Builder.Default
    private Integer carriedForwardDays = 0;
    @Column(name = "carry_forward_limit", nullable = false)
    @Builder.Default
    private Integer carryForwardLimit = 0;

    @Column(name = "settled_by", nullable = false, length = 50)
    private String settledBy;
    @Column(name = "settled_date", nullable = false)
    private LocalDateTime settledDate;
}