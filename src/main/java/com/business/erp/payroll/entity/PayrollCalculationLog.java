package com.business.erp.payroll.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable, append-only record of exactly how one employee's payslip was calculated
 * for one calculation version of one payroll run. Never updated or deleted — a
 * recalculation always inserts a new row. This is the source of truth for dispute
 * resolution: every number on a payslip must be traceable back to a row here.
 */
@Entity
@Table(name = "payroll_calculation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollCalculationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_run_id", nullable = false)
    private Long payrollRunId;
    @Column(name = "payroll_detail_id")
    private Long payrollDetailId;
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    @Column(name = "calculation_version", nullable = false)
    private Integer calculationVersion;

    @Column(name = "monthly_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlySalary;
    @Column(name = "hourly_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal hourlyRate;

    @Column(name = "present_days", nullable = false)
    @Builder.Default
    private Integer presentDays = 0;
    @Column(name = "weekly_off_days", nullable = false)
    @Builder.Default
    private Integer weeklyOffDays = 0;
    @Column(name = "weekly_off_worked_days", nullable = false)
    @Builder.Default
    private Integer weeklyOffWorkedDays = 0;
    @Column(name = "holiday_days", nullable = false)
    @Builder.Default
    private Integer holidayDays = 0;
    @Column(name = "holiday_worked_days", nullable = false)
    @Builder.Default
    private Integer holidayWorkedDays = 0;
    @Column(name = "paid_leave_days", nullable = false)
    @Builder.Default
    private Integer paidLeaveDays = 0;
    @Column(name = "automatic_paid_leave_days", nullable = false)
    @Builder.Default
    private Integer automaticPaidLeaveDays = 0;
    @Column(name = "absent_days", nullable = false)
    @Builder.Default
    private Integer absentDays = 0;

    @Column(name = "approved_ot_minutes", nullable = false)
    @Builder.Default
    private Integer approvedOtMinutes = 0;
    @Column(name = "holiday_ot_minutes", nullable = false)
    @Builder.Default
    private Integer holidayOtMinutes = 0;
    @Column(name = "weekly_off_ot_minutes", nullable = false)
    @Builder.Default
    private Integer weeklyOffOtMinutes = 0;

    @Column(name = "basic_salary_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal basicSalaryAmount = BigDecimal.ZERO;
    @Column(name = "overtime_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal overtimeAmount = BigDecimal.ZERO;
    @Column(name = "weekly_off_pay_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal weeklyOffPayAmount = BigDecimal.ZERO;
    @Column(name = "holiday_ot_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal holidayOtAmount = BigDecimal.ZERO;
    /** Mirrors PayrollDetail.performanceIncentiveAmount — same read-only value logged here for audit parity. */
    @Column(name = "performance_incentive_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal performanceIncentiveAmount = BigDecimal.ZERO;
    @Column(name = "leave_encashment_days", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal leaveEncashmentDays = BigDecimal.ZERO;
    @Column(name = "leave_encashment_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal leaveEncashmentAmount = BigDecimal.ZERO;
    @Column(name = "loss_of_pay_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal lossOfPayAmount = BigDecimal.ZERO;

    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grossSalary = BigDecimal.ZERO;
    @Column(name = "final_net_salary", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal finalNetSalary = BigDecimal.ZERO;

    @Column(name = "calculated_by", nullable = false, length = 50)
    private String calculatedBy;
    @Column(name = "calculated_date", nullable = false)
    private LocalDateTime calculatedDate;

    /** Newline-joined, human-readable explanation lines — see CalculationBreakdownBuilder. */
    @Column(name = "calculation_breakdown", columnDefinition = "TEXT")
    private String calculationBreakdown;

    /** Which {@code PayrollCalculationEngine} version produced this row — see item 4, Calculation Versioning. */
    @Column(name = "engine_version", length = 10)
    private String engineVersion;
}