package com.business.erp.employee.service;

import com.business.erp.employee.dto.request.CreateDesignationRequest;
import com.business.erp.employee.dto.request.UpdateDesignationRequest;
import com.business.erp.employee.dto.response.DesignationResponse;
import com.business.erp.employee.entity.DesignationMaster;

import java.util.List;

public interface DesignationMasterService {
    DesignationResponse create(CreateDesignationRequest request, String actor);
    DesignationResponse update(Long id, UpdateDesignationRequest request, String actor);
    DesignationResponse findById(Long id);
    List<DesignationResponse> findAll(boolean activeOnly);
    List<DesignationResponse> findByCategory(Long categoryId);
    DesignationMaster getEntityById(Long id);
}