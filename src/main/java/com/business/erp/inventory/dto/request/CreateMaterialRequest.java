package com.business.erp.inventory.dto.request;

import com.business.erp.inventory.entity.Material;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMaterialRequest {
    @NotBlank
    private String name;
    @NotNull
    private Material.MaterialUnit unit;
    @NotNull
    private Material.MaterialType materialType;
    private BigDecimal reorderLevel;
}
