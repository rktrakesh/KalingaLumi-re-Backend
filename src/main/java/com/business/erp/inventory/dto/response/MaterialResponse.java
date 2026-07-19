package com.business.erp.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MaterialResponse {
    private Long id;
    private String materialCode;
    private String name;
    private String unit;
    private String materialType;
    private BigDecimal reorderLevel;
    private BigDecimal currentStock;
    private String status;
    private boolean lowStock;
}
