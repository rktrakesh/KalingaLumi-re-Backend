package com.business.erp.payroll.entity;

import com.business.erp.payroll.enums.PayrollStatus;
import jakarta.persistence.*;
import lombok.*;
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
    private PayrollStatus status = PayrollStatus.DRAFT;

    // ── Versioning ───────────────────────────────────────────────────────
    @Column(name = "calculation_version", nullable = false)
    @Builder.Default
    private Integer calculationVersion = 1;
    @Column(name = "is_current_version", nullable = false)
    @Builder.Default
    private Boolean isCurrentVersion = true;
    @Column(name = "previous_run_id")
    private Long previousRunId;
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "total_employees")
    private Integer totalEmployees;
    @Column(name = "total_gross", precision = 15, scale = 2)
    private BigDecimal totalGross;
    @Column(name = "total_net", precision = 15, scale = 2)
    private BigDecimal totalNet;

    // ── Lifecycle audit trail ───────────────────────────────────────────
    @Column(name = "generated_by", nullable = false, length = 50)
    private String generatedBy;
    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

    @Column(name = "verified_by", length = 50)
    private String verifiedBy;
    @Column(name = "verified_date")
    private LocalDateTime verifiedDate;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "processed_by", length = 50)
    private String processedBy;
    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "locked_by", length = 50)
    private String lockedBy;
    @Column(name = "locked_date")
    private LocalDateTime lockedDate;

    @Column(name = "reopened_by", length = 50)
    private String reopenedBy;
    @Column(name = "reopened_date")
    private LocalDateTime reopenedDate;
    @Column(name = "reopen_reason", columnDefinition = "TEXT")
    private String reopenReason;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    // ── Convenience state checks ────────────────────────────────────────
    @Transient
    public boolean isAtLeast(PayrollStatus target) {
        return this.status.ordinal() >= target.ordinal();
    }
}