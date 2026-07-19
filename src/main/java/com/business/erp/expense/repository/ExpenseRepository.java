package com.business.erp.expense.repository;

import com.business.erp.expense.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:from IS NULL OR e.expenseDate >= :from) AND " +
            "(:to IS NULL OR e.expenseDate <= :to) " +
            "ORDER BY e.expenseDate DESC")
    Page<Expense> search(@Param("status") Expense.ExpenseStatus status,
                         @Param("category") Expense.ExpenseCategory category,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to,
                         Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status = 'APPROVED' " +
            "AND e.expenseDate BETWEEN :from AND :to AND (:category IS NULL OR e.category = :category)")
    BigDecimal sumApproved(@Param("from") LocalDate from, @Param("to") LocalDate to,
                           @Param("category") Expense.ExpenseCategory category);
}
