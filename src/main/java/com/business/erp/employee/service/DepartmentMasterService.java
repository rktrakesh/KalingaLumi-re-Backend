package com.business.erp.employee.service;

import com.business.erp.employee.dto.request.CreateMasterDataRequest;
import com.business.erp.employee.dto.request.UpdateMasterDataRequest;
import com.business.erp.employee.dto.response.MasterDataResponse;
import com.business.erp.employee.entity.DepartmentMaster;

import java.util.List;

public interface DepartmentMasterService {
    MasterDataResponse create(CreateMasterDataRequest request, String actor);
    MasterDataResponse update(Long id, UpdateMasterDataRequest request, String actor);
    MasterDataResponse findById(Long id);
    List<MasterDataResponse> findAll(boolean activeOnly);
    DepartmentMaster getEntityById(Long id);
}