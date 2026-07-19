package com.business.erp.supplier.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.supplier.dto.request.SupplierRequest;
import com.business.erp.supplier.dto.response.SupplierLedgerResponse;
import com.business.erp.supplier.dto.response.SupplierResponse;
import com.business.erp.supplier.entity.Supplier;
import com.business.erp.supplier.entity.SupplierLedger;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierService {
    SupplierResponse create(SupplierRequest request);

    SupplierResponse update(Long id, SupplierRequest request);

    PageResponse<SupplierResponse> search(Supplier.SupplierStatus status, String search, Pageable pageable);

    SupplierResponse findById(Long id);

    List<SupplierLedgerResponse> getLedger(Long id);

    void postLedger(Supplier supplier, SupplierLedger.SupplierTxType type, BigDecimal amount, Long refId, String remarks);

    Supplier getSupplier(Long id);
}