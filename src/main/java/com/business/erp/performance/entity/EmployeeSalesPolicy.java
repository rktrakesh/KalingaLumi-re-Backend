package com.business.erp.performance.entity;

import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.employee.entity.Employee;
import com.business.erp.performance.enums.SalesPolicyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee_sales_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSalesPolicy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "monthly_target", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyTarget;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private SalesPolicyStatus status = SalesPolicyStatus.ACTIVE;

    @OneToMany(mappedBy = "salesPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IncentiveSlab> incentiveSlabs = new ArrayList<>();
}