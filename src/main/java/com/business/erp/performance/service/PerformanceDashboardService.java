package com.business.erp.performance.service;

import com.business.erp.performance.dto.response.EmployeePerformanceDashboardResponse;
import com.business.erp.performance.dto.response.ManagementPerformanceDashboardResponse;

public interface PerformanceDashboardService {

    EmployeePerformanceDashboardResponse getEmployeeDashboard(Long employeeId, int year, int month);

    ManagementPerformanceDashboardResponse getManagementDashboard(int year, int month);
}