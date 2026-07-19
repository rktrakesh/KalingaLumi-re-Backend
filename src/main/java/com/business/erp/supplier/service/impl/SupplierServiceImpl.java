package com.business.erp.supplier.service.impl;

import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.supplier.dto.request.SupplierRequest;
import com.business.erp.supplier.dto.response.SupplierLedgerResponse;
import com.business.erp.supplier.dto.response.SupplierResponse;
import com.business.erp.supplier.entity.Supplier;
import com.business.erp.supplier.entity.SupplierLedger;
import com.business.erp.supplier.repository.SupplierLedgerRepository;
import com.business.erp.supplier.repository.SupplierRepository;
import com.business.erp.supplier.service.SupplierService;
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
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierLedgerRepository ledgerRepository;
    private final ReferenceNumberService refService;
    private final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);

    @Transactional
    public SupplierResponse create(SupplierRequest req) {
        log.info("SupplierServiceImpl:create :: invoked");
        log.debug("SupplierServiceImpl:create :: invoked");
        Supplier s = supplierRepository.save(Supplier.builder()
                .supplierCode(refService.generateSupplierCode())
                .name(req.getName()).phone(req.getPhone())
                .address(req.getAddress()).materialsSupplied(req.getMaterialsSupplied()).build());
        return toResponse(s, BigDecimal.ZERO);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest req) {
        log.info("SupplierServiceImpl:update :: invoked");
        log.debug("SupplierServiceImpl:update :: invoked");
        Supplier s = getSupplier(id);
        s.setName(req.getName());
        s.setPhone(req.getPhone());
        s.setAddress(req.getAddress());
        s.setMaterialsSupplied(req.getMaterialsSupplied());
        return toResponse(supplierRepository.save(s), getOutstanding(id));
    }

    public PageResponse<SupplierResponse> search(Supplier.SupplierStatus status, String search, Pageable pageable) {
        log.debug("SupplierServiceImpl:search :: invoked");
        return PageResponse.of(supplierRepository.search(status, search, pageable)
                .map(s -> toResponse(s, getOutstanding(s.getId()))));
    }

    public SupplierResponse findById(Long id) {
        log.debug("SupplierServiceImpl:findById :: invoked");
        return toResponse(getSupplier(id), getOutstanding(id));
    }

    public List<SupplierLedgerResponse> getLedger(Long id) {
        log.debug("SupplierServiceImpl:getLedger :: invoked");
        getSupplier(id);
        return ledgerRepository.findBySupplierIdOrderByTransactionDateAsc(id)
                .stream().map(this::toLedgerResponse).collect(Collectors.toList());
    }

    @Transactional
    public void postLedger(Supplier supplier, SupplierLedger.SupplierTxType type,
                           BigDecimal amount, Long refId, String remarks) {
        log.info("SupplierServiceImpl:method :: invoked");
        log.debug("SupplierServiceImpl:postLedger :: invoked");
        BigDecimal prev = getOutstanding(supplier.getId());
        BigDecimal newBal = type == SupplierLedger.SupplierTxType.PAYMENT ? prev.subtract(amount) : prev.add(amount);
        ledgerRepository.save(SupplierLedger.builder()
                .supplier(supplier).transactionDate(LocalDate.now())
                .transactionType(type).amount(amount).balanceAfter(newBal.max(BigDecimal.ZERO))
                .referenceId(refId).remarks(remarks).createdBy(getCurrentUser()).build());
    }

    public Supplier getSupplier(Long id) {
        log.debug("SupplierServiceImpl:getSupplier :: invoked");
        return supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    private BigDecimal getOutstanding(Long id) {
        return ledgerRepository.findLatest(id).map(SupplierLedger::getBalanceAfter).orElse(BigDecimal.ZERO);
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    private SupplierResponse toResponse(Supplier s, BigDecimal outstanding) {
        return SupplierResponse.builder()
                .id(s.getId()).supplierCode(s.getSupplierCode()).name(s.getName())
                .phone(s.getPhone()).address(s.getAddress())
                .materialsSupplied(s.getMaterialsSupplied()).status(s.getStatus().name())
                .outstandingPayable(outstanding).build();
    }

    private SupplierLedgerResponse toLedgerResponse(SupplierLedger l) {
        return SupplierLedgerResponse.builder()
                .id(l.getId()).transactionDate(l.getTransactionDate())
                .transactionType(l.getTransactionType().name())
                .amount(l.getAmount()).balanceAfter(l.getBalanceAfter())
                .remarks(l.getRemarks()).createdBy(l.getCreatedBy()).build();
    }
}
