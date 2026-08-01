package com.business.erp.performance.service;

import com.business.erp.performance.dto.request.LogCustomerVisitRequest;
import com.business.erp.performance.entity.CustomerVisit;

import java.util.List;

public interface CustomerVisitService {

    CustomerVisit logVisit(LogCustomerVisitRequest request, String actor);

    List<CustomerVisit> getVisitHistory(Long customerId);
}