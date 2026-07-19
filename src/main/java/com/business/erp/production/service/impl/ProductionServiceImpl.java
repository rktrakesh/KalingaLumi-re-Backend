package com.business.erp.production.service.impl;

import com.business.erp.auth.repository.UserRepository;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.inventory.service.InventoryService;
import com.business.erp.production.dto.request.CompleteBatchRequest;
import com.business.erp.production.dto.request.CreateBatchRequest;
import com.business.erp.production.dto.response.BatchResponse;
import com.business.erp.production.entity.ProductionBatch;
import com.business.erp.production.entity.ProductionInput;
import com.business.erp.production.entity.ProductionOutput;
import com.business.erp.production.repository.ProductionBatchRepository;
import com.business.erp.production.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionServiceImpl implements ProductionService {

    private final ProductionBatchRepository batchRepository;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;
    private final ReferenceNumberService refService;
    private final Logger log = LoggerFactory.getLogger(ProductionServiceImpl.class);

    @Transactional
    public BatchResponse createBatch(CreateBatchRequest req, String createdBy) {
        log.info("createBatch called by user={} batchDate={} inputs={}", createdBy,
                req != null ? req.getBatchDate() : null,
                req != null && req.getInputs() != null ? req.getInputs().size() : 0);
        log.debug("createBatch request={}", req);
        var manager = userRepository.findByUsername(createdBy).orElse(null);

        ProductionBatch batch = ProductionBatch.builder()
                .batchNumber(refService.generateBatchReference(req.getBatchDate()))
                .batchDate(req.getBatchDate()).manager(manager)
                .remarks(req.getRemarks()).build();

        if (req.getInputs() != null) {
            for (var inputReq : req.getInputs()) {
                var material = inventoryService.getMaterialEntity(inputReq.getMaterialId());
                batch.getInputs().add(ProductionInput.builder()
                        .batch(batch).material(material)
                        .quantityUsed(inputReq.getQuantityUsed()).build());
            }
        }

        var saved = batchRepository.save(batch);
        log.info("createBatch succeeded id={} batchNumber={}", saved.getId(), saved.getBatchNumber());
        return toResponse(saved);
    }

    @Transactional
    public BatchResponse completeBatch(Long id, CompleteBatchRequest req, String completedBy) {
        log.info("completeBatch called id={} by user={}", id, completedBy);
        log.debug("completeBatch request={}", req);
        ProductionBatch batch = getBatch(id);
        if (batch.getStatus() != ProductionBatch.BatchStatus.IN_PROGRESS) {
            log.error("completeBatch failed id={} status={}", id, batch.getStatus());
            throw new BusinessException("Only IN_PROGRESS batches can be completed");
        }

        // Deduct input materials from inventory
        for (ProductionInput input : batch.getInputs()) {
            inventoryService.recordConsumption(input.getMaterial(), input.getQuantityUsed(),
                    batch.getId(), batch.getBatchDate(), completedBy);
        }

        // Calculate total input for efficiency
        BigDecimal totalInput = batch.getInputs().stream()
                .map(ProductionInput::getQuantityUsed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add output to inventory (finished goods)
        for (var outReq : req.getOutputs()) {
            var material = inventoryService.getMaterialEntity(outReq.getMaterialId());
            BigDecimal finQty = outReq.getFinishedQuantity() != null ? outReq.getFinishedQuantity() : BigDecimal.ZERO;
            BigDecimal wasteQty = outReq.getWasteQuantity() != null ? outReq.getWasteQuantity() : BigDecimal.ZERO;

            BigDecimal efficiency = totalInput.compareTo(BigDecimal.ZERO) > 0
                    ? finQty.divide(totalInput, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            batch.getOutputs().add(ProductionOutput.builder()
                    .batch(batch).material(material)
                    .finishedQuantity(finQty).wasteQuantity(wasteQty)
                    .efficiencyPercent(efficiency).build());

            inventoryService.recordProduction(material, finQty,
                    batch.getId(), batch.getBatchDate(), completedBy);
        }

        batch.setStatus(ProductionBatch.BatchStatus.COMPLETED);
        if (req.getRemarks() != null) batch.setRemarks(req.getRemarks());
        var saved = batchRepository.save(batch);
        log.info("completeBatch succeeded id={} status={}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public BatchResponse cancelBatch(Long id, String remarks) {
        log.info("cancelBatch called id={} remarks={}", id, remarks);
        log.debug("cancelBatch params id={} remarks={}", id, remarks);
        ProductionBatch batch = getBatch(id);
        if (batch.getStatus() == ProductionBatch.BatchStatus.COMPLETED) {
            log.error("cancelBatch failed id={} status={}", id, batch.getStatus());
            throw new BusinessException("Completed batches cannot be cancelled");
        }
        batch.setStatus(ProductionBatch.BatchStatus.CANCELLED);
        batch.setRemarks(remarks);
        var saved = batchRepository.save(batch);
        log.info("cancelBatch succeeded id={} status={}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<BatchResponse> search(ProductionBatch.BatchStatus status,
                                              LocalDate from, LocalDate to, Pageable pageable) {
        log.info("search called status={} from={} to={} page={}", status, from, to, pageable);
        var page = batchRepository.search(status, from, to, pageable).map(this::toResponse);
        try {
            log.info("search returned totalElements={}", page.getTotalElements());
        } catch (Exception ex) {
            // defensive: some Page implementations may lazy-evaluate
            log.debug("search result totalElements not available: {}", ex.getMessage());
        }
        return PageResponse.of(page);
    }

    public BatchResponse findById(Long id) {
        log.info("findById called id={}", id);
        var batch = getBatch(id);
        log.info("findById succeeded id={} batchNumber={}", batch.getId(), batch.getBatchNumber());
        return toResponse(batch);
    }

    private ProductionBatch getBatch(Long id) {
        var opt = batchRepository.findById(id);
        if (opt.isEmpty()) {
            log.error("getBatch not found id={}", id);
            throw new ResourceNotFoundException("ProductionBatch", id);
        }
        return opt.get();
    }

    private BatchResponse toResponse(ProductionBatch b) {
        return BatchResponse.builder()
                .id(b.getId()).batchNumber(b.getBatchNumber()).batchDate(b.getBatchDate())
                .managerName(b.getManager() != null ? b.getManager().getFullName() : null)
                .status(b.getStatus().name()).remarks(b.getRemarks())
                .inputs(b.getInputs().stream().map(i -> BatchResponse.InputResponse.builder()
                        .materialId(i.getMaterial().getId()).materialName(i.getMaterial().getName())
                        .quantityUsed(i.getQuantityUsed()).build()).collect(Collectors.toList()))
                .outputs(b.getOutputs().stream().map(o -> BatchResponse.OutputResponse.builder()
                        .materialId(o.getMaterial().getId()).materialName(o.getMaterial().getName())
                        .finishedQuantity(o.getFinishedQuantity()).wasteQuantity(o.getWasteQuantity())
                        .efficiencyPercent(o.getEfficiencyPercent()).build()).collect(Collectors.toList()))
                .build();
    }
}
