package com.business.erp.performance.entity;

import com.business.erp.employee.entity.Employee;
import com.business.erp.performance.enums.PerformanceSnapshotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Set once Payroll consumes this snapshot during generation — null until then. */
    @Column(name = "payroll_run_id")
    private Long payrollRunId;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "sales_policy_id")
    private Long salesPolicyId;

    @Column(name = "sales_policy_version")
    private Integer salesPolicyVersion;

    @Column(name = "monthly_target", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyTarget;

    @Column(name = "actual_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal actualSales;

    @Column(name = "achievement_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal achievementPct;

    @Column(name = "incentive_slab_id")
    private Long incentiveSlabId;

    @Column(name = "incentive_pct_applied", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal incentivePctApplied = BigDecimal.ZERO;

    @Column(name = "incentive_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal incentiveAmount = BigDecimal.ZERO;

    @Column(name = "assigned_customer_count", nullable = false)
    @Builder.Default
    private Integer assignedCustomerCount = 0;

    @Column(name = "active_customer_count", nullable = false)
    @Builder.Default
    private Integer activeCustomerCount = 0;

    @Column(name = "orders_count", nullable = false)
    @Builder.Default
    private Integer ordersCount = 0;

    @Column(name = "total_order_value", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalOrderValue = BigDecimal.ZERO;

    @Column(name = "average_order_value", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal averageOrderValue = BigDecimal.ZERO;

    @Column(name = "new_customers_count", nullable = false)
    @Builder.Default
    private Integer newCustomersCount = 0;

    @Column(name = "repeat_customers_count", nullable = false)
    @Builder.Default
    private Integer repeatCustomersCount = 0;

    @Column(name = "recommendation_code", length = 40)
    private String recommendationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private PerformanceSnapshotStatus status = PerformanceSnapshotStatus.DRAFT;

    @Column(name = "engine_version", nullable = false, length = 20)
    private String engineVersion;

    @Column(name = "generated_by", length = 50)
    private String generatedBy;

    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
}