package com.business.erp.sales.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.sales.dto.request.CreateSaleRequest;
import com.business.erp.sales.dto.request.SalePaymentRequest;
import com.business.erp.sales.dto.request.SaleReturnRequest;
import com.business.erp.sales.dto.response.SaleResponse;
import com.business.erp.sales.entity.SalesInvoice;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SalesService {
    SaleResponse create(CreateSaleRequest request, String createdBy);

    SaleResponse recordPayment(Long id, SalePaymentRequest request, String createdBy);

    SaleResponse processReturn(Long id, SaleReturnRequest request, String createdBy);

    PageResponse<SaleResponse> search(Long customerId, SalesInvoice.PaymentStatus status, LocalDate from, LocalDate to, Pageable pageable);

    SaleResponse findById(Long id);
}