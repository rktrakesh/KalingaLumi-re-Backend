package com.business.erp.production.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.production.dto.request.CompleteBatchRequest;
import com.business.erp.production.dto.request.CreateBatchRequest;
import com.business.erp.production.dto.response.BatchResponse;
import com.business.erp.production.entity.ProductionBatch;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ProductionService {
    BatchResponse createBatch(CreateBatchRequest request, String createdBy);

    BatchResponse completeBatch(Long id, CompleteBatchRequest request, String completedBy);

    BatchResponse cancelBatch(Long id, String remarks);

    PageResponse<BatchResponse> search(ProductionBatch.BatchStatus status, LocalDate from, LocalDate to, Pageable pageable);

    BatchResponse findById(Long id);
}