package com.business.erp.payroll.entity;

import com.business.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "calculation_version", nullable = false)
    @Builder.Default
    private Integer calculationVersion = 1;

    // Snapshot fields
    @Column(name = "employee_code", nullable = false, length = 20)
    private String employeeCode;
    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;
    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;
    @Column(name = "standard_work_days", nullable = false)
    private Integer standardWorkDays;
    @Column(name = "standard_work_hours", nullable = false)
    private Integer standardWorkHours;
    @Column(name = "hourly_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal hourlyRate;

    // Day-classification counts (from the payroll calendar engine)
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
    @Column(name = "automatic_paid_leave_days", nullable = false)
    @Builder.Default
    private Integer automaticPaidLeaveDays = 0;
    @Column(name = "absent_days", nullable = false)
    @Builder.Default
    private Integer absentDays = 0;

    // Computed fields
    @Column(name = "worked_minutes", nullable = false)
    private Integer workedMinutes;
    @Column(name = "paid_leave_days", nullable = false)
    @Builder.Default
    private Integer paidLeaveDays = 0;
    @Column(name = "overtime_minutes", nullable = false)
    @Builder.Default
    private Integer overtimeMinutes = 0;
    @Column(name = "overtime_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal overtimeMultiplier;
    @Column(name = "holiday_ot_minutes", nullable = false)
    @Builder.Default
    private Integer holidayOtMinutes = 0;
    @Column(name = "weekly_off_ot_minutes", nullable = false)
    @Builder.Default
    private Integer weeklyOffOtMinutes = 0;
    @Column(name = "weekly_off_multiplier", nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal weeklyOffMultiplier = BigDecimal.valueOf(1.5);
    @Column(name = "holiday_ot_multiplier", nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal holidayOtMultiplier = BigDecimal.valueOf(2.0);

    @Column(name = "weekly_off_pay", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal weeklyOffPay = BigDecimal.ZERO;
    @Column(name = "holiday_ot_pay", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal holidayOtPay = BigDecimal.ZERO;
    /** Stored directly (mirrors weeklyOffPay/holidayOtPay) so nothing downstream re-derives a PayrollCalculationEngine formula. */
    @Column(name = "overtime_pay", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal overtimePay = BigDecimal.ZERO;
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
    private BigDecimal grossSalary;
    @Column(name = "loan_interest_deduction", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal loanInterestDeduction = BigDecimal.ZERO;
    @Column(name = "loan_principal_deduction", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal loanPrincipalDeduction = BigDecimal.ZERO;
    @Column(name = "total_deductions", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;
    @Column(name = "salary_capped", nullable = false)
    @Builder.Default
    private Boolean salaryCapped = false;

    // Payment tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_mode", length = 10)
    private String paymentMode;

    @Column(name = "paid_by", length = 50)
    private String paidBy;

    // Audit
    @Column(name = "created_by", length = 50)
    private String createdBy;
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @PrePersist
    public void prePersist() {
        if (createdDate == null) createdDate = LocalDateTime.now();
    }

    public enum PaymentStatus {PENDING, PAID}
}