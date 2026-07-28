package com.business.erp.payroll.repository;

import com.business.erp.payroll.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    /** The single "live" version for a period, if any. */
    Optional<PayrollRun> findByYearAndMonthAndIsCurrentVersionTrue(int year, int month);

    boolean existsByYearAndMonthAndIsCurrentVersionTrue(int year, int month);

    /** Every version ever generated for a period, oldest first — the full version history. */
    List<PayrollRun> findByYearAndMonthOrderByCalculationVersionAsc(int year, int month);

    /** All current-version runs, latest period first — what dashboards/listing screens show. */
    List<PayrollRun> findByIsCurrentVersionTrueOrderByYearDescMonthDesc();

    @Query("SELECT MAX(p.calculationVersion) FROM PayrollRun p WHERE p.year = :year AND p.month = :month")
    Integer findMaxVersion(@Param("year") int year, @Param("month") int month);

    List<PayrollRun> findAllByOrderByYearDescMonthDesc();
}
