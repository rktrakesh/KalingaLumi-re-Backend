package com.business.erp.inventory.service.impl;

import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.InsufficientStockException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.inventory.dto.request.CreateMaterialRequest;
import com.business.erp.inventory.dto.request.StockAdjustmentRequest;
import com.business.erp.inventory.dto.response.InventoryLedgerResponse;
import com.business.erp.inventory.dto.response.MaterialResponse;
import com.business.erp.inventory.entity.InventoryLedger;
import com.business.erp.inventory.entity.Material;
import com.business.erp.inventory.repository.InventoryLedgerRepository;
import com.business.erp.inventory.repository.MaterialRepository;
import com.business.erp.inventory.service.InventoryService;
import com.business.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final MaterialRepository materialRepository;
    private final InventoryLedgerRepository ledgerRepository;
    private final ReferenceNumberService refService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    @Transactional
    public MaterialResponse createMaterial(CreateMaterialRequest req) {
        log.info("InventoryServiceImpl:createMaterial :: invoked");
        log.debug("InventoryServiceImpl:createMaterial :: invoked");
        Material m = materialRepository.save(Material.builder()
                .materialCode(refService.generateMaterialCode())
                .name(req.getName()).unit(req.getUnit()).materialType(req.getMaterialType())
                .reorderLevel(req.getReorderLevel() != null ? req.getReorderLevel() : BigDecimal.ZERO)
                .build());
        return toMaterialResponse(m, BigDecimal.ZERO);
    }

    public List<MaterialResponse> getAllMaterials() {
        log.debug("InventoryServiceImpl:getAllMaterials :: invoked");
        return materialRepository.findByStatus(Material.MaterialStatus.ACTIVE)
                .stream().map(m -> toMaterialResponse(m, getCurrentStock(m.getId())))
                .collect(Collectors.toList());
    }

    public MaterialResponse getMaterial(Long id) {
        log.debug("InventoryServiceImpl:getMaterial :: invoked");
        Material m = getMaterialEntity(id);
        return toMaterialResponse(m, getCurrentStock(id));
    }

    @Transactional
    public InventoryLedgerResponse adjust(StockAdjustmentRequest req, String adjustedBy) {
        log.info("InventoryServiceImpl:adjust :: invoked");
        log.debug("InventoryServiceImpl:adjust :: invoked");
        Material m = getMaterialEntity(req.getMaterialId());
        BigDecimal current = getCurrentStock(req.getMaterialId());
        BigDecimal newBal = current.add(req.getQuantity());
        if (newBal.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("Adjustment would make stock negative. Current: " + current);

        InventoryLedger entry = post(m, req.getAdjustmentDate(),
                InventoryLedger.InventoryTxType.ADJUSTMENT, req.getQuantity(),
                req.getUnitCost(), "MANUAL", null, req.getRemarks(), adjustedBy);

        auditService.log("INVENTORY", "ADJUSTMENT", "Material", m.getId(),
                "stock=" + current, "stock=" + newBal);
        checkLowStock(m, newBal);
        return toLedgerResponse(entry);
    }

    /**
     * Called by Purchase module when a purchase is saved.
     */
    @Transactional
    public void recordPurchase(Material material, BigDecimal qty, BigDecimal unitCost,
                               Long purchaseId, LocalDate date, String createdBy) {
        log.info("InventoryServiceImpl:recordPurchase :: invoked");
        log.debug("InventoryServiceImpl:recordPurchase :: invoked");
        post(material, date, InventoryLedger.InventoryTxType.PURCHASE,
                qty, unitCost, "PURCHASE", purchaseId, "Purchase receipt", createdBy);
        checkLowStock(material, getCurrentStock(material.getId()));
    }

    /**
     * Called by Production module on batch completion.
     */
    @Transactional
    public void recordConsumption(Material material, BigDecimal qty, Long batchId,
                                  LocalDate date, String createdBy) {
        log.info("InventoryServiceImpl:recordConsumption :: invoked");
        log.debug("InventoryServiceImpl:recordConsumption :: invoked");
        BigDecimal current = getCurrentStock(material.getId());
        if (current.compareTo(qty) < 0)
            throw new InsufficientStockException(material.getName(),
                    current.doubleValue(), qty.doubleValue());
        post(material, date, InventoryLedger.InventoryTxType.PRODUCTION_CONSUMPTION,
                qty.negate(), null, "BATCH", batchId, "Production consumption", createdBy);
        checkLowStock(material, getCurrentStock(material.getId()));
    }

    /**
     * Called by Production module to add finished goods.
     */
    @Transactional
    public void recordProduction(Material material, BigDecimal qty, Long batchId,
                                 LocalDate date, String createdBy) {
        log.info("InventoryServiceImpl:recordProduction :: invoked");
        log.debug("InventoryServiceImpl:recordProduction :: invoked");
        post(material, date, InventoryLedger.InventoryTxType.PRODUCTION_CONSUMPTION,
                qty, null, "BATCH", batchId, "Finished goods production", createdBy);
    }

    /**
     * Called by Sales module on invoice creation.
     */
    @Transactional
    public void recordSale(Material material, BigDecimal qty, Long invoiceId,
                           LocalDate date, String createdBy) {
        log.info("InventoryServiceImpl:recordSale :: invoked");
        log.debug("InventoryServiceImpl:recordSale :: invoked");
        BigDecimal current = getCurrentStock(material.getId());
        if (current.compareTo(qty) < 0)
            throw new InsufficientStockException(material.getName(),
                    current.doubleValue(), qty.doubleValue());
        post(material, date, InventoryLedger.InventoryTxType.SALE,
                qty.negate(), null, "SALE_INVOICE", invoiceId, "Sales dispatch", createdBy);
    }

    /**
     * Called by Sales module on return.
     */
    @Transactional
    public void recordReturn(Material material, BigDecimal qty, Long invoiceId,
                             LocalDate date, String createdBy) {
        log.info("InventoryServiceImpl:recordReturn :: invoked");
        log.debug("InventoryServiceImpl:recordReturn :: invoked");
        post(material, date, InventoryLedger.InventoryTxType.RETURN,
                qty, null, "SALE_INVOICE", invoiceId, "Sales return", createdBy);
    }

    public PageResponse<InventoryLedgerResponse> getLedger(Long materialId, Pageable pageable) {
        log.debug("InventoryServiceImpl:getLedger :: invoked");
        getMaterialEntity(materialId);
        return PageResponse.of(ledgerRepository
                .findByMaterialIdOrderByTransactionDateDescIdDesc(materialId, pageable)
                .map(this::toLedgerResponse));
    }

    public List<MaterialResponse> getLowStockAlerts() {
        log.debug("InventoryServiceImpl:getLowStockAlerts :: invoked");
        return materialRepository.findByStatus(Material.MaterialStatus.ACTIVE).stream()
                .map(m -> {
                    BigDecimal stock = getCurrentStock(m.getId());
                    if (stock.compareTo(m.getReorderLevel()) <= 0)
                        return toMaterialResponse(m, stock);
                    return null;
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    public BigDecimal getCurrentStock(Long materialId) {
        log.debug("InventoryServiceImpl:getCurrentStock :: invoked");
        return ledgerRepository.findLatestByMaterial(materialId)
                .map(InventoryLedger::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
    }

    public Material getMaterialEntity(Long id) {
        log.debug("InventoryServiceImpl:getMaterialEntity :: invoked");
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
    }

    private void checkLowStock(Material m, BigDecimal newStock) {
        if (m.getReorderLevel().compareTo(BigDecimal.ZERO) > 0
                && newStock.compareTo(m.getReorderLevel()) <= 0) {
            notificationService.createLowInventoryNotification(m.getName(), m.getId());
        }
    }

    private InventoryLedger post(Material material, LocalDate date,
                                 InventoryLedger.InventoryTxType type, BigDecimal qty,
                                 BigDecimal unitCost, String refType, Long refId,
                                 String remarks, String createdBy) {
        BigDecimal prev = getCurrentStock(material.getId());
        BigDecimal newBal = prev.add(qty);
        return ledgerRepository.save(InventoryLedger.builder()
                .material(material).transactionDate(date).transactionType(type)
                .quantity(qty).balanceAfter(newBal.max(BigDecimal.ZERO))
                .unitCost(unitCost).referenceType(refType).referenceId(refId)
                .remarks(remarks).createdBy(createdBy).build());
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    private MaterialResponse toMaterialResponse(Material m, BigDecimal stock) {
        return MaterialResponse.builder()
                .id(m.getId()).materialCode(m.getMaterialCode()).name(m.getName())
                .unit(m.getUnit().name()).materialType(m.getMaterialType().name())
                .reorderLevel(m.getReorderLevel()).currentStock(stock)
                .status(m.getStatus().name())
                .lowStock(m.getReorderLevel().compareTo(BigDecimal.ZERO) > 0
                        && stock.compareTo(m.getReorderLevel()) <= 0)
                .build();
    }

    private InventoryLedgerResponse toLedgerResponse(InventoryLedger il) {
        return InventoryLedgerResponse.builder()
                .id(il.getId()).transactionDate(il.getTransactionDate())
                .transactionType(il.getTransactionType().name())
                .quantity(il.getQuantity()).balanceAfter(il.getBalanceAfter())
                .unitCost(il.getUnitCost()).referenceType(il.getReferenceType())
                .referenceId(il.getReferenceId()).remarks(il.getRemarks())
                .createdBy(il.getCreatedBy()).build();
    }
}
