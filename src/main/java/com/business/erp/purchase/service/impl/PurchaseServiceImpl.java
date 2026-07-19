package com.business.erp.purchase.service.impl;

import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.inventory.service.InventoryService;
import com.business.erp.purchase.dto.request.CreatePurchaseRequest;
import com.business.erp.purchase.dto.request.PurchasePaymentRequest;
import com.business.erp.purchase.dto.response.PurchaseResponse;
import com.business.erp.purchase.entity.Purchase;
import com.business.erp.purchase.entity.PurchaseItem;
import com.business.erp.purchase.entity.PurchasePayment;
import com.business.erp.purchase.repository.PurchaseRepository;
import com.business.erp.purchase.service.PurchaseService;
import com.business.erp.supplier.entity.SupplierLedger;
import com.business.erp.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierService supplierService;
    private final InventoryService inventoryService;
    private final CashbookService cashbookService;
    private final ReferenceNumberService refService;
    private final Logger log = LoggerFactory.getLogger(PurchaseServiceImpl.class);

    @Transactional
    public PurchaseResponse create(CreatePurchaseRequest req, String createdBy) {
        log.info("PurchaseServiceImpl:create :: invoked");
        log.debug("PurchaseServiceImpl:create :: invoked");
        var supplier = supplierService.getSupplier(req.getSupplierId());

        Purchase purchase = Purchase.builder()
                .purchaseReference(refService.generatePurchaseReference(req.getPurchaseDate()))
                .supplier(supplier).purchaseDate(req.getPurchaseDate())
                .remarks(req.getRemarks()).build();

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : req.getItems()) {
            var material = inventoryService.getMaterialEntity(itemReq.getMaterialId());
            BigDecimal lineTotal = itemReq.getQuantity().multiply(itemReq.getUnitRate())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            PurchaseItem item = PurchaseItem.builder()
                    .purchase(purchase).material(material)
                    .quantity(itemReq.getQuantity()).unitRate(itemReq.getUnitRate())
                    .totalAmount(lineTotal).build();
            purchase.getItems().add(item);
            total = total.add(lineTotal);
        }

        purchase.setTotalAmount(total);
        purchase.setOutstandingAmount(total);
        Purchase saved = purchaseRepository.save(purchase);

        // Update inventory
        for (PurchaseItem item : saved.getItems()) {
            inventoryService.recordPurchase(item.getMaterial(), item.getQuantity(),
                    item.getUnitRate(), saved.getId(), saved.getPurchaseDate(), createdBy);
        }

        // Update supplier ledger
        supplierService.postLedger(supplier, SupplierLedger.SupplierTxType.PURCHASE,
                total, saved.getId(), "Purchase: " + saved.getPurchaseReference());

        return toResponse(saved);
    }

    @Transactional
    public PurchaseResponse recordPayment(Long id, PurchasePaymentRequest req, String createdBy) {
        log.info("PurchaseServiceImpl:recordPayment :: invoked");
        log.debug("PurchaseServiceImpl:recordPayment :: invoked");
        Purchase purchase = getPurchase(id);
        if (purchase.getStatus() == Purchase.PurchaseStatus.CANCELLED)
            throw new BusinessException("Cannot pay for a cancelled purchase");

        BigDecimal remaining = purchase.getOutstandingAmount();
        if (req.getAmount().compareTo(remaining) > 0)
            throw new BusinessException("Payment amount exceeds outstanding: " + remaining);

        PurchasePayment payment = PurchasePayment.builder()
                .purchase(purchase).paymentDate(req.getPaymentDate())
                .amount(req.getAmount()).paymentMode(req.getPaymentMode())
                .remarks(req.getRemarks()).createdBy(createdBy).build();
        purchase.getPayments().add(payment);

        purchase.setPaidAmount(purchase.getPaidAmount().add(req.getAmount()));
        purchase.setOutstandingAmount(remaining.subtract(req.getAmount()));
        purchase.setPaymentStatus(
                purchase.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0
                        ? Purchase.PaymentStatus.PAID : Purchase.PaymentStatus.PARTIAL);
        Purchase saved = purchaseRepository.save(purchase);

        supplierService.postLedger(purchase.getSupplier(),
                SupplierLedger.SupplierTxType.PAYMENT, req.getAmount(),
                id, "Payment for: " + purchase.getPurchaseReference());

        cashbookService.recordSupplierPayment(req.getAmount(),
                purchase.getPurchaseReference(), purchase.getSupplier().getName(), id);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseResponse> search(Long supplierId, Purchase.PaymentStatus status,
                                                 LocalDate from, LocalDate to, Pageable pageable) {
        log.debug("PurchaseServiceImpl:search :: invoked");
        return PageResponse.of(purchaseRepository.search(supplierId, status, from, to, pageable)
                .map(this::toResponse));
    }

    public PurchaseResponse findById(Long id) {
        log.debug("PurchaseServiceImpl:findById :: invoked");
        return toResponse(getPurchase(id));
    }

    private Purchase getPurchase(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", id));
    }

    private PurchaseResponse toResponse(Purchase p) {
        return PurchaseResponse.builder()
                .id(p.getId()).purchaseReference(p.getPurchaseReference())
                .supplierId(p.getSupplier().getId()).supplierName(p.getSupplier().getName())
                .purchaseDate(p.getPurchaseDate()).totalAmount(p.getTotalAmount())
                .paidAmount(p.getPaidAmount()).outstandingAmount(p.getOutstandingAmount())
                .paymentStatus(p.getPaymentStatus().name()).status(p.getStatus().name())
                .remarks(p.getRemarks())
                .items(p.getItems().stream().map(i -> PurchaseResponse.PurchaseItemResponse.builder()
                                .id(i.getId()).materialId(i.getMaterial().getId())
                                .materialName(i.getMaterial().getName()).quantity(i.getQuantity())
                                .unitRate(i.getUnitRate()).totalAmount(i.getTotalAmount()).build())
                        .collect(Collectors.toList()))
                .build();
    }
}
