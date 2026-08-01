package com.business.erp.sales.service.impl;

import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.customer.entity.Customer;
import com.business.erp.customer.entity.CustomerLedger;
import com.business.erp.customer.service.CustomerService;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.inventory.service.InventoryService;
import com.business.erp.performance.service.CustomerOwnershipService;
import com.business.erp.sales.dto.request.CreateSaleRequest;
import com.business.erp.sales.dto.request.SalePaymentRequest;
import com.business.erp.sales.dto.request.SaleReturnRequest;
import com.business.erp.sales.dto.response.SaleResponse;
import com.business.erp.sales.entity.SalesInvoice;
import com.business.erp.sales.entity.SalesInvoiceItem;
import com.business.erp.sales.entity.SalesPayment;
import com.business.erp.sales.entity.SalesReturn;
import com.business.erp.sales.repository.SalesInvoiceRepository;
import com.business.erp.sales.service.SalesService;
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
public class SalesServiceImpl implements SalesService {

    private final SalesInvoiceRepository invoiceRepository;
    private final CustomerService customerService;
    private final InventoryService inventoryService;
    private final CashbookService cashbookService;
    private final ReferenceNumberService refService;
    private final EmployeeService employeeService;
    private final CustomerOwnershipService customerOwnershipService;
    private final Logger log = LoggerFactory.getLogger(SalesServiceImpl.class);

    @Transactional
    public SaleResponse create(CreateSaleRequest req, String createdBy) {
        log.info("SalesServiceImpl:method :: invoked");
        log.debug("SalesServiceImpl:create :: invoked");
        Customer customer = customerService.getCustomer(req.getCustomerId());
        LocalDate dueDate = req.getInvoiceDate().plusDays(customer.getCreditDays());

        Employee soldBy = req.getSoldByEmployeeId() != null ? employeeService.getEmployee(req.getSoldByEmployeeId()) : null;
        Employee creditedTo = customerOwnershipService.resolveOwnerAsOf(customer.getId(), req.getInvoiceDate())
                .orElse(soldBy);

        SalesInvoice invoice = SalesInvoice.builder()
                .invoiceReference(refService.generateSaleReference(req.getInvoiceDate()))
                .customer(customer).invoiceDate(req.getInvoiceDate()).dueDate(dueDate)
                .soldBy(soldBy).creditedTo(creditedTo)
                .remarks(req.getRemarks()).build();

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : req.getItems()) {
            var material = inventoryService.getMaterialEntity(itemReq.getMaterialId());
            BigDecimal lineTotal = itemReq.getQuantityKg().multiply(itemReq.getUnitRate())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            invoice.getItems().add(SalesInvoiceItem.builder()
                    .invoice(invoice).material(material)
                    .quantityKg(itemReq.getQuantityKg()).unitRate(itemReq.getUnitRate())
                    .totalAmount(lineTotal).build());
            total = total.add(lineTotal);
        }

        invoice.setTotalAmount(total);
        invoice.setOutstandingAmount(total);
        SalesInvoice saved = invoiceRepository.save(invoice);

        // Deduct from inventory
        for (SalesInvoiceItem item : saved.getItems()) {
            inventoryService.recordSale(item.getMaterial(), item.getQuantityKg(),
                    saved.getId(), saved.getInvoiceDate(), createdBy);
        }

        // Post to customer ledger
        customerService.postLedger(customer, CustomerLedger.CustomerTxType.INVOICE,
                total, dueDate, saved.getId(), "Invoice: " + saved.getInvoiceReference());

        return toResponse(saved);
    }

    @Transactional
    public SaleResponse recordPayment(Long id, SalePaymentRequest req, String createdBy) {
        log.info("SalesServiceImpl:method :: invoked");
        log.debug("SalesServiceImpl:recordPayment :: invoked");
        SalesInvoice invoice = getInvoice(id);
        if (invoice.getStatus() == SalesInvoice.InvoiceStatus.CANCELLED)
            throw new BusinessException("Cannot pay a cancelled invoice");
        if (req.getAmount().compareTo(invoice.getOutstandingAmount()) > 0)
            throw new BusinessException("Payment exceeds outstanding: " + invoice.getOutstandingAmount());

        invoice.getPayments().add(SalesPayment.builder()
                .invoice(invoice).paymentDate(req.getPaymentDate())
                .amount(req.getAmount()).paymentMode(req.getPaymentMode())
                .remarks(req.getRemarks()).createdBy(createdBy).build());

        invoice.setPaidAmount(invoice.getPaidAmount().add(req.getAmount()));
        invoice.setOutstandingAmount(invoice.getOutstandingAmount().subtract(req.getAmount()));
        invoice.setPaymentStatus(
                invoice.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0
                        ? SalesInvoice.PaymentStatus.PAID : SalesInvoice.PaymentStatus.PARTIAL);
        SalesInvoice saved = invoiceRepository.save(invoice);

        customerService.postLedger(invoice.getCustomer(), CustomerLedger.CustomerTxType.PAYMENT,
                req.getAmount(), null, id, "Payment for: " + invoice.getInvoiceReference());

        cashbookService.recordCustomerPayment(req.getAmount(),
                invoice.getInvoiceReference(), invoice.getCustomer().getName(), id);

        return toResponse(saved);
    }

    @Transactional
    public SaleResponse processReturn(Long id, SaleReturnRequest req, String createdBy) {
        log.info("SalesServiceImpl:method :: invoked");
        log.debug("SalesServiceImpl:processReturn :: invoked");
        SalesInvoice invoice = getInvoice(id);
        var material = inventoryService.getMaterialEntity(req.getMaterialId());

        invoice.getReturns().add(SalesReturn.builder()
                .invoice(invoice).returnDate(req.getReturnDate()).material(material)
                .quantityReturned(req.getQuantityReturned()).returnAmount(req.getReturnAmount())
                .remarks(req.getRemarks()).createdBy(createdBy).build());

        invoiceRepository.save(invoice);

        inventoryService.recordReturn(material, req.getQuantityReturned(),
                id, req.getReturnDate(), createdBy);

        customerService.postLedger(invoice.getCustomer(), CustomerLedger.CustomerTxType.RETURN,
                req.getReturnAmount(), null, id, "Return for: " + invoice.getInvoiceReference());

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> search(Long customerId, SalesInvoice.PaymentStatus status,
                                             LocalDate from, LocalDate to, Pageable pageable) {
        log.debug("SalesServiceImpl:search :: invoked");
        return PageResponse.of(invoiceRepository.search(customerId, status, from, to, pageable)
                .map(this::toResponse));
    }

    public SaleResponse findById(Long id) {
        log.debug("SalesServiceImpl:findById :: invoked");
        return toResponse(getInvoice(id));
    }

    private SalesInvoice getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesInvoice", id));
    }

    private SaleResponse toResponse(SalesInvoice s) {
        return SaleResponse.builder()
                .id(s.getId()).invoiceReference(s.getInvoiceReference())
                .customerId(s.getCustomer().getId()).customerName(s.getCustomer().getName())
                .invoiceDate(s.getInvoiceDate()).dueDate(s.getDueDate())
                .totalAmount(s.getTotalAmount()).paidAmount(s.getPaidAmount())
                .outstandingAmount(s.getOutstandingAmount())
                .paymentStatus(s.getPaymentStatus().name()).status(s.getStatus().name())
                .remarks(s.getRemarks())
                .items(s.getItems().stream().map(i -> SaleResponse.SaleItemResponse.builder()
                                .id(i.getId()).materialId(i.getMaterial().getId())
                                .materialName(i.getMaterial().getName()).quantityKg(i.getQuantityKg())
                                .unitRate(i.getUnitRate()).totalAmount(i.getTotalAmount()).build())
                        .collect(Collectors.toList()))
                .build();
    }
}