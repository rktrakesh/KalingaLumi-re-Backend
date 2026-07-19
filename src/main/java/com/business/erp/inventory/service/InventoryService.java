package com.business.erp.inventory.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.inventory.dto.request.CreateMaterialRequest;
import com.business.erp.inventory.dto.request.StockAdjustmentRequest;
import com.business.erp.inventory.dto.response.InventoryLedgerResponse;
import com.business.erp.inventory.dto.response.MaterialResponse;
import com.business.erp.inventory.entity.Material;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryService {
    MaterialResponse createMaterial(CreateMaterialRequest request);

    List<MaterialResponse> getAllMaterials();

    MaterialResponse getMaterial(Long id);

    InventoryLedgerResponse adjust(StockAdjustmentRequest request, String adjustedBy);

    void recordPurchase(Material material, BigDecimal qty, BigDecimal unitCost, Long purchaseId, LocalDate date, String createdBy);

    void recordConsumption(Material material, BigDecimal qty, Long batchId, LocalDate date, String createdBy);

    void recordProduction(Material material, BigDecimal qty, Long batchId, LocalDate date, String createdBy);

    void recordSale(Material material, BigDecimal qty, Long invoiceId, LocalDate date, String createdBy);

    void recordReturn(Material material, BigDecimal qty, Long invoiceId, LocalDate date, String createdBy);

    PageResponse<InventoryLedgerResponse> getLedger(Long materialId, Pageable pageable);

    List<MaterialResponse> getLowStockAlerts();

    BigDecimal getCurrentStock(Long materialId);

    Material getMaterialEntity(Long id);
}