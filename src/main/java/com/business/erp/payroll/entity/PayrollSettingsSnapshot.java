package com.business.erp.payroll.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable snapshot of every payroll-relevant {@link com.business.erp.settings.entity.AppSetting}
 * value captured at the moment payroll for a month is first generated. All recalculations
 * of that run (and its future versions) MUST use this snapshot rather than re-reading
 * current settings, so that a settings change never rewrites the past.
 */
@Entity
@Table(name = "payroll_settings_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollSettingsSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_run_id", nullable = false, unique = true)
    private Long payrollRunId;

    @Column(name = "standard_working_days", nullable = false)
    private Integer standardWorkingDays;
    @Column(name = "working_hours_per_day", nullable = false)
    private Integer workingHoursPerDay;
    @Column(name = "paid_leaves_per_month", nullable = false)
    private Integer paidLeavesPerMonth;

    @Column(name = "overtime_multiplier", nullable = false, precision = 6, scale = 3)
    private BigDecimal overtimeMultiplier;
    @Column(name = "weekly_off_multiplier", nullable = false, precision = 6, scale = 3)
    private BigDecimal weeklyOffMultiplier;
    @Column(name = "holiday_ot_multiplier", nullable = false, precision = 6, scale = 3)
    private BigDecimal holidayOtMultiplier;

    /** Comma-separated ISO day-of-week numbers (1=Mon..7=Sun), e.g. "7" or "6,7". */
    @Column(name = "weekly_off_days", nullable = false, length = 20)
    private String weeklyOffDays;

    @Column(name = "leave_allocation_method", nullable = false, length = 30)
    private String leaveAllocationMethod;
    @Column(name = "unused_leave_policy", nullable = false, length = 30)
    private String unusedLeavePolicy;
    @Column(name = "leave_carry_forward_limit", nullable = false)
    private Integer leaveCarryForwardLimit;
    @Column(name = "leave_encashment_enabled", nullable = false)
    private Boolean leaveEncashmentEnabled;

    @Column(name = "captured_by", nullable = false, length = 50)
    private String capturedBy;
    @Column(name = "captured_date", nullable = false)
    private LocalDateTime capturedDate;
}