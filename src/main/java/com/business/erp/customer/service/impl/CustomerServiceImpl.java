package com.business.erp.customer.service.impl;

import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.customer.dto.request.CustomerRequest;
import com.business.erp.customer.dto.response.CustomerLedgerResponse;
import com.business.erp.customer.dto.response.CustomerResponse;
import com.business.erp.customer.entity.Customer;
import com.business.erp.customer.entity.CustomerLedger;
import com.business.erp.customer.repository.CustomerLedgerRepository;
import com.business.erp.customer.repository.CustomerRepository;
import com.business.erp.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
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
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository ledgerRepository;
    private final ReferenceNumberService refService;
    private final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        log.info("CustomerServiceImpl:create :: invoked");
        log.debug("CustomerServiceImpl:create :: invoked");
        Customer c = customerRepository.save(Customer.builder()
                .customerCode(refService.generateCustomerCode())
                .name(req.getName()).phone(req.getPhone()).address(req.getAddress())
                .gstNumber(req.getGstNumber())
                .creditDays(req.getCreditDays() != null ? req.getCreditDays() : 30).build());
        return toResponse(c, BigDecimal.ZERO);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        log.info("CustomerServiceImpl:update :: invoked");
        log.debug("CustomerServiceImpl:update :: invoked");
        Customer c = getCustomer(id);
        c.setName(req.getName());
        c.setPhone(req.getPhone());
        c.setAddress(req.getAddress());
        c.setGstNumber(req.getGstNumber());
        if (req.getCreditDays() != null) c.setCreditDays(req.getCreditDays());
        return toResponse(customerRepository.save(c), getOutstanding(id));
    }

    public PageResponse<CustomerResponse> search(Customer.CustomerStatus status, String search, Pageable pageable) {
        log.debug("CustomerServiceImpl:search :: invoked");
        return PageResponse.of(customerRepository.search(status, search, pageable)
                .map(c -> toResponse(c, getOutstanding(c.getId()))));
    }

    public CustomerResponse findById(Long id) {
        log.debug("CustomerServiceImpl:findById :: invoked");
        return toResponse(getCustomer(id), getOutstanding(id));
    }

    public List<CustomerLedgerResponse> getLedger(Long id) {
        log.debug("CustomerServiceImpl:getLedger :: invoked");
        getCustomer(id);
        return ledgerRepository.findByCustomerIdOrderByTransactionDateAsc(id)
                .stream().map(this::toLedgerResponse).collect(Collectors.toList());
    }

    public List<CustomerResponse> getOutstandingList() {
        log.debug("CustomerServiceImpl:getOutstandingList :: invoked");
        return customerRepository.findByStatus(Customer.CustomerStatus.ACTIVE).stream()
                .map(c -> {
                    BigDecimal bal = getOutstanding(c.getId());
                    return bal.compareTo(BigDecimal.ZERO) > 0 ? toResponse(c, bal) : null;
                })
                .filter(r -> r != null).collect(Collectors.toList());
    }

    @Transactional
    public void postLedger(Customer customer, CustomerLedger.CustomerTxType type,
                           BigDecimal amount, LocalDate dueDate, Long refId, String remarks) {
        log.info("CustomerServiceImpl:method :: invoked");
        log.debug("CustomerServiceImpl:postLedger :: invoked");
        BigDecimal prev = getOutstanding(customer.getId());
        BigDecimal newBal = type == CustomerLedger.CustomerTxType.PAYMENT || type == CustomerLedger.CustomerTxType.RETURN
                ? prev.subtract(amount) : prev.add(amount);
        ledgerRepository.save(CustomerLedger.builder()
                .customer(customer).transactionDate(LocalDate.now())
                .transactionType(type).amount(amount).balanceAfter(newBal.max(BigDecimal.ZERO))
                .dueDate(dueDate).referenceId(refId).remarks(remarks).createdBy(getCurrentUser()).build());
    }

    public Customer getCustomer(Long id) {
        log.debug("CustomerServiceImpl:getCustomer :: invoked");
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    public BigDecimal getOutstanding(Long id) {
        log.debug("CustomerServiceImpl:getOutstanding :: invoked");
        return ledgerRepository.findLatest(id).map(CustomerLedger::getBalanceAfter).orElse(BigDecimal.ZERO);
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    private CustomerResponse toResponse(Customer c, BigDecimal outstanding) {
        return CustomerResponse.builder()
                .id(c.getId()).customerCode(c.getCustomerCode()).name(c.getName())
                .phone(c.getPhone()).address(c.getAddress()).gstNumber(c.getGstNumber())
                .creditDays(c.getCreditDays()).status(c.getStatus().name())
                .outstandingReceivable(outstanding).build();
    }

    private CustomerLedgerResponse toLedgerResponse(CustomerLedger l) {
        return CustomerLedgerResponse.builder()
                .id(l.getId()).transactionDate(l.getTransactionDate())
                .transactionType(l.getTransactionType().name())
                .amount(l.getAmount()).balanceAfter(l.getBalanceAfter())
                .dueDate(l.getDueDate()).remarks(l.getRemarks()).createdBy(l.getCreatedBy()).build();
    }
}
