package com.business.erp.performance.service;

import com.business.erp.performance.dto.request.CreateSalesPolicyRequest;
import com.business.erp.performance.entity.EmployeeSalesPolicy;

import java.util.List;

public interface EmployeeSalesPolicyService {

    EmployeeSalesPolicy createPolicy(CreateSalesPolicyRequest request, String actor);

    EmployeeSalesPolicy getActivePolicy(Long employeeId);

    List<EmployeeSalesPolicy> getPolicyHistory(Long employeeId);
}