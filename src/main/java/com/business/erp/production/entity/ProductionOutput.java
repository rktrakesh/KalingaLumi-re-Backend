package com.business.erp.production.entity;

import com.business.erp.inventory.entity.Material;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "production_outputs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOutput {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductionBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "finished_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal finishedQuantity;

    @Column(name = "waste_quantity", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal wasteQuantity = BigDecimal.ZERO;

    @Column(name = "efficiency_percent", precision = 5, scale = 2)
    private BigDecimal efficiencyPercent;
}
