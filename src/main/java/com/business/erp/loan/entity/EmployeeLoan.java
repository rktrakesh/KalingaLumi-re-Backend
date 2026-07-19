package com.business.erp.loan.entity;

import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLoan extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_reference", nullable = false, unique = true, length = 20)
    private String loanReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "principal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "monthly_interest", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyInterest;

    @Column(name = "monthly_principal_payment", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrincipalPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING_APPROVAL;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public enum LoanStatus {PENDING_APPROVAL, ACTIVE, CLOSED, REJECTED}
}
