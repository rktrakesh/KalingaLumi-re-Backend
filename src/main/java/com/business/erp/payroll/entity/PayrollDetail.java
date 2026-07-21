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