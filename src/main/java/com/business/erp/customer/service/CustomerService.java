package com.business.erp.customer.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.customer.dto.request.CustomerRequest;
import com.business.erp.customer.dto.response.CustomerLedgerResponse;
import com.business.erp.customer.dto.response.CustomerResponse;
import com.business.erp.customer.entity.Customer;
import com.business.erp.customer.entity.CustomerLedger;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CustomerService {
    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(Long id, CustomerRequest request);

    PageResponse<CustomerResponse> search(Customer.CustomerStatus status, String search, Pageable pageable);

    CustomerResponse findById(Long id);

    List<CustomerLedgerResponse> getLedger(Long id);

    List<CustomerResponse> getOutstandingList();

    void postLedger(Customer customer, CustomerLedger.CustomerTxType type, BigDecimal amount, LocalDate dueDate, Long refId, String remarks);

    Customer getCustomer(Long id);

    BigDecimal getOutstanding(Long id);
}