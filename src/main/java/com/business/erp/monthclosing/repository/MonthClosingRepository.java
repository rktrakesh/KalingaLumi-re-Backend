package com.business.erp.monthclosing.repository;

import com.business.erp.monthclosing.entity.MonthClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthClosingRepository extends JpaRepository<MonthClosing, Long> {
    Optional<MonthClosing> findByYearAndMonth(int year, int month);

    List<MonthClosing> findAllByOrderByYearDescMonthDesc();
}
