package com.business.erp.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incentive_slabs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncentiveSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_policy_id", nullable = false)
    private EmployeeSalesPolicy salesPolicy;

    @Column(name = "min_achievement_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal minAchievementPct;

    /** Null = unbounded top slab (e.g. "Above 150%"). */
    @Column(name = "max_achievement_pct", precision = 6, scale = 2)
    private BigDecimal maxAchievementPct;

    @Column(name = "incentive_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal incentivePct;

    @Column(name = "slab_order", nullable = false)
    private Integer slabOrder;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}