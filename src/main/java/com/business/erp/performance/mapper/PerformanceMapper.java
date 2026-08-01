package com.business.erp.performance.mapper;

import com.business.erp.performance.dto.response.CustomerOwnershipResponse;
import com.business.erp.performance.dto.response.CustomerVisitResponse;
import com.business.erp.performance.dto.response.PerformanceSnapshotResponse;
import com.business.erp.performance.dto.response.SalesPolicyResponse;
import com.business.erp.performance.entity.CustomerOwnership;
import com.business.erp.performance.entity.CustomerVisit;
import com.business.erp.performance.entity.EmployeeSalesPolicy;
import com.business.erp.performance.entity.PerformanceSnapshot;
import com.business.erp.performance.recommendation.RecommendationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PerformanceMapper {

    private final RecommendationEngine recommendationEngine;

    public SalesPolicyResponse toResponse(EmployeeSalesPolicy p) {
        return SalesPolicyResponse.builder()
                .id(p.getId()).employeeId(p.getEmployee().getId()).employeeName(p.getEmployee().getName())
                .monthlyTarget(p.getMonthlyTarget())
                .effectiveFrom(p.getEffectiveFrom()).effectiveTo(p.getEffectiveTo())
                .version(p.getVersion()).status(p.getStatus().name())
                .slabs(p.getIncentiveSlabs().stream().map(s -> SalesPolicyResponse.SlabResponse.builder()
                                .id(s.getId()).minAchievementPct(s.getMinAchievementPct())
                                .maxAchievementPct(s.getMaxAchievementPct()).incentivePct(s.getIncentivePct())
                                .slabOrder(s.getSlabOrder()).build())
                        .toList())
                .build();
    }

    public List<SalesPolicyResponse> toResponseList(List<EmployeeSalesPolicy> policies) {
        return policies.stream().map(this::toResponse).toList();
    }

    public CustomerOwnershipResponse toResponse(CustomerOwnership o) {
        return CustomerOwnershipResponse.builder()
                .id(o.getId()).customerId(o.getCustomer().getId()).customerName(o.getCustomer().getName())
                .employeeId(o.getEmployee().getId()).employeeName(o.getEmployee().getName())
                .effectiveFrom(o.getEffectiveFrom()).effectiveTo(o.getEffectiveTo())
                .isTemporary(o.getIsTemporary()).status(o.getStatus().name()).remarks(o.getRemarks())
                .build();
    }

    public List<CustomerOwnershipResponse> toOwnershipResponseList(List<CustomerOwnership> ownerships) {
        return ownerships.stream().map(this::toResponse).toList();
    }

    public CustomerVisitResponse toResponse(CustomerVisit v) {
        return CustomerVisitResponse.builder()
                .id(v.getId()).customerId(v.getCustomer().getId()).customerName(v.getCustomer().getName())
                .visitedByEmployeeId(v.getVisitedBy().getId()).visitedByEmployeeName(v.getVisitedBy().getName())
                .visitDate(v.getVisitDate()).visitPurpose(v.getVisitPurpose().name())
                .visitOutcome(v.getVisitOutcome().name()).remarks(v.getRemarks())
                .build();
    }

    public List<CustomerVisitResponse> toVisitResponseList(List<CustomerVisit> visits) {
        return visits.stream().map(this::toResponse).toList();
    }

    public PerformanceSnapshotResponse toResponse(PerformanceSnapshot s) {
        var rec = recommendationEngine.recommend(s.getAchievementPct());
        return PerformanceSnapshotResponse.builder()
                .id(s.getId()).employeeId(s.getEmployee().getId()).employeeName(s.getEmployee().getName())
                .payrollRunId(s.getPayrollRunId())
                .periodYear(s.getPeriodYear()).periodMonth(s.getPeriodMonth())
                .salesPolicyVersion(s.getSalesPolicyVersion())
                .monthlyTarget(s.getMonthlyTarget()).actualSales(s.getActualSales())
                .achievementPct(s.getAchievementPct())
                .incentivePctApplied(s.getIncentivePctApplied()).incentiveAmount(s.getIncentiveAmount())
                .assignedCustomerCount(s.getAssignedCustomerCount()).activeCustomerCount(s.getActiveCustomerCount())
                .ordersCount(s.getOrdersCount()).totalOrderValue(s.getTotalOrderValue())
                .averageOrderValue(s.getAverageOrderValue())
                .newCustomersCount(s.getNewCustomersCount()).repeatCustomersCount(s.getRepeatCustomersCount())
                .recommendationCode(s.getRecommendationCode())
                .recommendationLabel(rec.label()).recommendationSuggestion(rec.suggestion())
                .status(s.getStatus().name()).engineVersion(s.getEngineVersion())
                .generatedBy(s.getGeneratedBy()).generatedDate(s.getGeneratedDate())
                .approvedBy(s.getApprovedBy()).approvedDate(s.getApprovedDate())
                .build();
    }

    public List<PerformanceSnapshotResponse> toSnapshotResponseList(List<PerformanceSnapshot> snapshots) {
        return snapshots.stream().map(this::toResponse).toList();
    }
}