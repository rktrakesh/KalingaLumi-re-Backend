package com.business.erp.common.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ReferenceNumberService {

    @PersistenceContext
    private EntityManager em;
    private final Logger log = LoggerFactory.getLogger(ReferenceNumberService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateEmployeeCode() {
        String ref = generate("seq_employee", "EMP-%05d");
        log.debug("ReferenceNumberService:generateEmployeeCode :: generated={}", ref);
        return ref;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateLoanReference() {
        return generate("seq_loan", "LOAN-%05d");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateSupplierCode() {
        return generate("seq_supplier", "SUP-%05d");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateCustomerCode() {
        return generate("seq_customer", "CUST-%05d");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateMaterialCode() {
        return generate("seq_material", "MAT-%05d");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateExpenseReference(LocalDate date) {
        return generateWithYearMonth("seq_expense", "EXP", date);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generatePayrollReference(LocalDate date) {
        return generateWithYearMonth("seq_payroll", "PAY", date);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateSaleReference(LocalDate date) {
        return generateWithYearMonth("seq_sale", "SALE", date);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generatePurchaseReference(LocalDate date) {
        return generateWithYearMonth("seq_purchase", "PUR", date);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateBatchReference(LocalDate date) {
        return generateWithYearMonth("seq_batch", "BATCH", date);
    }

    @SuppressWarnings("unchecked")
    private String generate(String table, String pattern) {
        Long next = ((Number) em.createNativeQuery("SELECT next_val FROM " + table + " FOR UPDATE")
                .getSingleResult()).longValue();
        em.createNativeQuery("UPDATE " + table + " SET next_val = next_val + 1").executeUpdate();
        return String.format(pattern, next);
    }

    @SuppressWarnings("unchecked")
    private String generateWithYearMonth(String table, String prefix, LocalDate date) {
        Long next = ((Number) em.createNativeQuery("SELECT next_val FROM " + table + " FOR UPDATE")
                .getSingleResult()).longValue();
        em.createNativeQuery("UPDATE " + table + " SET next_val = next_val + 1").executeUpdate();
        return String.format("%s-%04d-%02d-%05d", prefix, date.getYear(), date.getMonthValue(), next);
    }
}
