package com.business.erp.performance.service.impl;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.enums.EmployeeCategoryCode;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.performance.dto.request.CreateSalesPolicyRequest;
import com.business.erp.performance.entity.EmployeeSalesPolicy;
import com.business.erp.performance.entity.IncentiveSlab;
import com.business.erp.performance.enums.SalesPolicyStatus;
import com.business.erp.performance.repository.EmployeeSalesPolicyRepository;
import com.business.erp.performance.service.EmployeeSalesPolicyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeSalesPolicyServiceImpl implements EmployeeSalesPolicyService {

    private final EmployeeSalesPolicyRepository salesPolicyRepository;
    private final EmployeeService employeeService;
    private final Logger log = LoggerFactory.getLogger(EmployeeSalesPolicyServiceImpl.class);

    @Override
    @Transactional
    public EmployeeSalesPolicy createPolicy(CreateSalesPolicyRequest request, String actor) {
        Employee employee = employeeService.getEmployee(request.getEmployeeId());
        if (!EmployeeCategoryCode.SALES.equals(employee.getEmployeeCategory().getCode())) {
            throw new BusinessException("NOT_SALES_EMPLOYEE: only SALES-category employees can have a sales policy " +
                    "(employeeId=" + request.getEmployeeId() + " is " + employee.getEmployeeCategory().getCode() + ")");
        }
        validateSlabs(request.getSlabs());

        Optional<EmployeeSalesPolicy> current = salesPolicyRepository
                .findByEmployeeIdAndStatus(request.getEmployeeId(), SalesPolicyStatus.ACTIVE);

        current.ifPresent(previous -> {
            previous.setStatus(SalesPolicyStatus.SUPERSEDED);
            previous.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
            salesPolicyRepository.save(previous);
        });

        int nextVersion = current.map(p -> p.getVersion() + 1).orElse(1);

        EmployeeSalesPolicy policy = EmployeeSalesPolicy.builder()
                .employee(employee).monthlyTarget(request.getMonthlyTarget())
                .effectiveFrom(request.getEffectiveFrom()).effectiveTo(null)
                .version(nextVersion).status(SalesPolicyStatus.ACTIVE)
                .build();
        policy.setCreatedDate(LocalDateTime.now());

        List<IncentiveSlab> slabs = new ArrayList<>();
        for (CreateSalesPolicyRequest.SlabRequest slabReq : request.getSlabs()) {
            slabs.add(IncentiveSlab.builder()
                    .salesPolicy(policy)
                    .minAchievementPct(slabReq.getMinAchievementPct())
                    .maxAchievementPct(slabReq.getMaxAchievementPct())
                    .incentivePct(slabReq.getIncentivePct())
                    .slabOrder(slabReq.getSlabOrder())
                    .createdBy(actor).createdDate(LocalDateTime.now())
                    .build());
        }
        policy.setIncentiveSlabs(slabs);

        EmployeeSalesPolicy saved = salesPolicyRepository.save(policy);
        log.info("EmployeeSalesPolicyServiceImpl:createPolicy :: empId={} version={} target={} slabs={} by={}",
                request.getEmployeeId(), nextVersion, request.getMonthlyTarget(), slabs.size(), actor);
        return saved;
    }

    @Override
    public EmployeeSalesPolicy getActivePolicy(Long employeeId) {
        return salesPolicyRepository.findByEmployeeIdAndStatus(employeeId, SalesPolicyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active sales policy for employeeId=" + employeeId));
    }

    @Override
    public List<EmployeeSalesPolicy> getPolicyHistory(Long employeeId) {
        return salesPolicyRepository.findByEmployeeIdOrderByVersionDesc(employeeId);
    }

    private void validateSlabs(List<CreateSalesPolicyRequest.SlabRequest> slabs) {
        for (CreateSalesPolicyRequest.SlabRequest slab : slabs) {
            if (slab.getMaxAchievementPct() != null &&
                    slab.getMaxAchievementPct().compareTo(slab.getMinAchievementPct()) < 0) {
                throw new BusinessException("INVALID_SLAB: maxAchievementPct must be >= minAchievementPct " +
                        "(slabOrder=" + slab.getSlabOrder() + ")");
            }
        }
    }
}