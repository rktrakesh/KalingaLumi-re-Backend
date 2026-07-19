package com.business.erp.purchase.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.purchase.dto.request.CreatePurchaseRequest;
import com.business.erp.purchase.dto.request.PurchasePaymentRequest;
import com.business.erp.purchase.dto.response.PurchaseResponse;
import com.business.erp.purchase.entity.Purchase;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PurchaseService {
    PurchaseResponse create(CreatePurchaseRequest request, String createdBy);

    PurchaseResponse recordPayment(Long id, PurchasePaymentRequest request, String createdBy);

    PageResponse<PurchaseResponse> search(Long supplierId, Purchase.PaymentStatus status, LocalDate from, LocalDate to, Pageable pageable);

    PurchaseResponse findById(Long id);
}