package com.business.erp.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_reference", nullable = false, unique = true, length = 30)
    private String runReference;

    @Column(nullable = false)
    private Integer year;
    @Column(nullable = false)
    private Integer month;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.GENERATED;

    @Column(name = "total_employees")
    private Integer totalEmployees;
    @Column(name = "total_gross", precision = 15, scale = 2)
    private BigDecimal totalGross;
    @Column(name = "total_net", precision = 15, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "generated_by", nullable = false, length = 50)
    private String generatedBy;
    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

    @Column(name = "locked_by", length = 50)
    private String lockedBy;
    @Column(name = "locked_date")
    private LocalDateTime lockedDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public enum PayrollStatus {GENERATED, REGENERATED, LOCKED}
}
