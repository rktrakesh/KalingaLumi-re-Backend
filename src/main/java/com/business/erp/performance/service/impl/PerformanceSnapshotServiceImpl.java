package com.business.erp.performance.service.impl;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.enums.EmployeeCategoryCode;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.performance.engine.PerformanceCalculationContext;
import com.business.erp.performance.engine.PerformanceCalculationEngine;
import com.business.erp.performance.engine.PerformanceCalculationResult;
import com.business.erp.performance.engine.PerformanceEngineVersion;
import com.business.erp.performance.entity.EmployeeSalesPolicy;
import com.business.erp.performance.entity.IncentiveSlab;
import com.business.erp.performance.entity.PerformanceSnapshot;
import com.business.erp.performance.enums.PerformanceSnapshotStatus;
import com.business.erp.performance.recommendation.PerformanceRecommendation;
import com.business.erp.performance.recommendation.RecommendationEngine;
import com.business.erp.performance.repository.EmployeeSalesPolicyRepository;
import com.business.erp.performance.repository.IncentiveSlabRepository;
import com.business.erp.performance.repository.PerformanceSnapshotRepository;
import com.business.erp.performance.service.CustomerOwnershipService;
import com.business.erp.performance.service.PerformanceLiveMetrics;
import com.business.erp.performance.service.PerformanceSnapshotService;
import com.business.erp.sales.entity.SalesInvoice;
import com.business.erp.sales.entity.SalesReturn;
import com.business.erp.sales.repository.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceSnapshotServiceImpl implements PerformanceSnapshotService {

    private final PerformanceSnapshotRepository snapshotRepository;
    private final EmployeeSalesPolicyRepository salesPolicyRepository;
    private final IncentiveSlabRepository incentiveSlabRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerOwnershipService customerOwnershipService;
    private final EmployeeService employeeService;
    private final PerformanceCalculationEngine calculationEngine;
    private final RecommendationEngine recommendationEngine;
    private final Logger log = LoggerFactory.getLogger(PerformanceSnapshotServiceImpl.class);

    @Override
    @Transactional
    public PerformanceSnapshot generate(Long employeeId, int year, int month, String generatedBy) {
        Optional<PerformanceSnapshot> existing =
                snapshotRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, year, month);
        existing.ifPresent(s -> {
            if (s.getStatus() == PerformanceSnapshotStatus.APPROVED) {
                throw new BusinessException("SNAPSHOT_ALREADY_APPROVED: performance snapshot for employeeId=" +
                        employeeId + " " + year + "-" + month + " is already approved and immutable");
            }
        });

        PerformanceLiveMetrics metrics = computeLiveMetrics(employeeId, year, month);
        PerformanceCalculationResult result = metrics.result();
        EmployeeSalesPolicy policy = metrics.policy();

        PerformanceSnapshot snapshot = existing.orElseGet(PerformanceSnapshot::new);
        snapshot.setEmployee(metrics.employee());
        snapshot.setPeriodYear(year);
        snapshot.setPeriodMonth(month);
        snapshot.setSalesPolicyId(policy.getId());
        snapshot.setSalesPolicyVersion(policy.getVersion());
        snapshot.setMonthlyTarget(policy.getMonthlyTarget());
        snapshot.setActualSales(metrics.actualSales());
        snapshot.setAchievementPct(result.getAchievementPct());
        snapshot.setIncentiveSlabId(result.getAppliedSlabId());
        snapshot.setIncentivePctApplied(result.getIncentivePctApplied());
        snapshot.setIncentiveAmount(result.getIncentiveAmount());
        snapshot.setAssignedCustomerCount(metrics.assignedCustomerCount());
        snapshot.setActiveCustomerCount(metrics.activeCustomerCount());
        snapshot.setOrdersCount(metrics.ordersCount());
        snapshot.setTotalOrderValue(metrics.totalOrderValue());
        snapshot.setAverageOrderValue(metrics.averageOrderValue());
        snapshot.setNewCustomersCount(metrics.newCustomersCount());
        snapshot.setRepeatCustomersCount(metrics.repeatCustomersCount());
        snapshot.setRecommendationCode(metrics.recommendation().code());
        snapshot.setStatus(PerformanceSnapshotStatus.DRAFT);
        snapshot.setEngineVersion(PerformanceEngineVersion.CURRENT);
        snapshot.setGeneratedBy(generatedBy);
        snapshot.setGeneratedDate(LocalDateTime.now());

        PerformanceSnapshot saved = snapshotRepository.save(snapshot);
        log.info("PerformanceSnapshotServiceImpl:generate :: empId={} period={}-{} achievement={}% incentive={} by={}",
                employeeId, year, month, result.getAchievementPct(), result.getIncentiveAmount(), generatedBy);
        return saved;
    }

    @Override
    public PerformanceLiveMetrics computeLiveMetrics(Long employeeId, int year, int month) {
        Employee employee = employeeService.getEmployee(employeeId);
        LocalDate periodStart = YearMonth.of(year, month).atDay(1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        EmployeeSalesPolicy policy = salesPolicyRepository.findPolicyAsOf(employeeId, periodEnd)
                .orElseThrow(() -> new BusinessException("NO_SALES_POLICY: no sales policy configured for employeeId=" +
                        employeeId + " covering " + year + "-" + month));
        List<IncentiveSlab> slabs = incentiveSlabRepository.findBySalesPolicyIdOrderBySlabOrderAsc(policy.getId());

        List<SalesInvoice> invoices = salesInvoiceRepository.findCreditedInvoicesForPeriod(employeeId, periodStart, periodEnd);

        BigDecimal actualSales = BigDecimal.ZERO;
        BigDecimal grossOrderValue = BigDecimal.ZERO;
        BigDecimal largestOrder = BigDecimal.ZERO;
        for (SalesInvoice inv : invoices) {
            BigDecimal returnsTotal = inv.getReturns().stream()
                    .map(SalesReturn::getReturnAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            actualSales = actualSales.add(inv.getTotalAmount()).subtract(returnsTotal);
            grossOrderValue = grossOrderValue.add(inv.getTotalAmount());
            if (inv.getTotalAmount().compareTo(largestOrder) > 0) largestOrder = inv.getTotalAmount();
        }
        actualSales = actualSales.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        int ordersCount = invoices.size();
        BigDecimal averageOrderValue = ordersCount == 0 ? BigDecimal.ZERO
                : grossOrderValue.divide(BigDecimal.valueOf(ordersCount), 2, RoundingMode.HALF_UP);

        Set<Long> distinctCustomerIds = invoices.stream()
                .map(inv -> inv.getCustomer().getId()).collect(Collectors.toSet());

        int newCustomers = 0, repeatCustomers = 0;
        for (Long customerId : distinctCustomerIds) {
            boolean hadEarlierOrder = salesInvoiceRepository
                    .existsByCustomerIdAndInvoiceDateBeforeAndStatus(customerId, periodStart, SalesInvoice.InvoiceStatus.ACTIVE);
            if (hadEarlierOrder) repeatCustomers++; else newCustomers++;
        }

        int assignedCustomerCount = customerOwnershipService.getAssignedCustomers(employeeId).size();
        BigDecimal collectionPending = salesInvoiceRepository.sumOutstandingForEmployee(employeeId);

        PerformanceCalculationResult result = calculationEngine.calculate(PerformanceCalculationContext.builder()
                .employeeId(employeeId).monthlyTarget(policy.getMonthlyTarget())
                .actualSales(actualSales).slabs(slabs).build());

        PerformanceRecommendation recommendation = recommendationEngine.recommend(result.getAchievementPct());

        return new PerformanceLiveMetrics(employee, policy, result, recommendation, actualSales,
                assignedCustomerCount, distinctCustomerIds.size(), ordersCount,
                grossOrderValue.setScale(2, RoundingMode.HALF_UP), averageOrderValue,
                newCustomers, repeatCustomers, collectionPending, largestOrder);
    }

    @Override
    @Transactional
    public List<PerformanceSnapshot> generateForAllSalesEmployees(int year, int month, String generatedBy) {
        List<Employee> salesEmployees = employeeService.getAttendanceEligibleEmployees().stream()
                .filter(e -> EmployeeCategoryCode.SALES.equals(e.getEmployeeCategory().getCode()))
                .toList();

        List<PerformanceSnapshot> generated = new ArrayList<>(salesEmployees.size());
        for (Employee emp : salesEmployees) {
            try {
                generated.add(generate(emp.getId(), year, month, generatedBy));
            } catch (Exception ex) {
                // One sales employee's failure (e.g. no policy configured yet) never blocks the rest.
                log.warn("PerformanceSnapshotServiceImpl:generateForAllSalesEmployees :: SKIPPED empId={} reason={}",
                        emp.getId(), ex.getMessage());
            }
        }
        log.info("PerformanceSnapshotServiceImpl:generateForAllSalesEmployees :: period={}-{} generated={}/{} by={}",
                year, month, generated.size(), salesEmployees.size(), generatedBy);
        return generated;
    }

    @Override
    @Transactional
    public PerformanceSnapshot approve(Long snapshotId, String approvedBy) {
        PerformanceSnapshot snapshot = fetch(snapshotId);
        if (snapshot.getStatus() == PerformanceSnapshotStatus.APPROVED) {
            throw new BusinessException("ALREADY_APPROVED: snapshotId=" + snapshotId);
        }
        snapshot.setStatus(PerformanceSnapshotStatus.APPROVED);
        snapshot.setApprovedBy(approvedBy);
        snapshot.setApprovedDate(LocalDateTime.now());
        PerformanceSnapshot saved = snapshotRepository.save(snapshot);
        log.info("PerformanceSnapshotServiceImpl:approve :: snapshotId={} empId={} period={}-{} incentive={} by={}",
                snapshotId, saved.getEmployee().getId(), saved.getPeriodYear(), saved.getPeriodMonth(),
                saved.getIncentiveAmount(), approvedBy);
        return saved;
    }

    @Override
    @Transactional
    public PerformanceSnapshot linkToPayrollRun(Long snapshotId, Long payrollRunId) {
        PerformanceSnapshot snapshot = fetch(snapshotId);
        snapshot.setPayrollRunId(payrollRunId);
        return snapshotRepository.save(snapshot);
    }

    @Override
    public Optional<BigDecimal> getApprovedIncentiveAmount(Long employeeId, int year, int month) {
        return snapshotRepository
                .findByEmployeeIdAndPeriodYearAndPeriodMonthAndStatus(employeeId, year, month, PerformanceSnapshotStatus.APPROVED)
                .map(PerformanceSnapshot::getIncentiveAmount);
    }

    @Override
    public Optional<PerformanceSnapshot> findByEmployeeAndPeriod(Long employeeId, int year, int month) {
        return snapshotRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, year, month);
    }

    @Override
    public List<PerformanceSnapshot> findByPeriod(int year, int month) {
        return snapshotRepository.findByPeriodYearAndPeriodMonth(year, month);
    }

    @Override
    public List<PerformanceSnapshot> findHistoryForEmployee(Long employeeId) {
        return snapshotRepository.findByEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(employeeId);
    }

    private PerformanceSnapshot fetch(Long id) {
        return snapshotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PerformanceSnapshot", id));
    }
}