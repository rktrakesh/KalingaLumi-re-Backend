package com.business.erp.employee.service.impl;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.dto.request.CreateMasterDataRequest;
import com.business.erp.employee.dto.request.UpdateMasterDataRequest;
import com.business.erp.employee.dto.response.MasterDataResponse;
import com.business.erp.employee.entity.EmployeeCategoryMaster;
import com.business.erp.employee.repository.EmployeeCategoryMasterRepository;
import com.business.erp.employee.service.EmployeeCategoryMasterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeCategoryMasterServiceImpl implements EmployeeCategoryMasterService {

    private final EmployeeCategoryMasterRepository repository;
    private final Logger log = LoggerFactory.getLogger(EmployeeCategoryMasterServiceImpl.class);

    @Override
    @Transactional
    public MasterDataResponse create(CreateMasterDataRequest request, String actor) {
        if (repository.existsByCode(request.getCode())) {
            throw new BusinessException("DUPLICATE_CODE: an employee category with code '" + request.getCode() + "' already exists");
        }
        EmployeeCategoryMaster entity = EmployeeCategoryMaster.builder()
                .code(request.getCode()).name(request.getName()).description(request.getDescription())
                .active(true).build();
        entity.setCreatedBy(actor);
        entity.setCreatedDate(LocalDateTime.now());
        EmployeeCategoryMaster saved = repository.save(entity);
        log.info("EmployeeCategoryMasterServiceImpl:create :: code={} by={}", request.getCode(), actor);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MasterDataResponse update(Long id, UpdateMasterDataRequest request, String actor) {
        EmployeeCategoryMaster entity = getEntityById(id);
        // code is never touched here — immutable by convention.
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getActive() != null) entity.setActive(request.getActive());
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(LocalDateTime.now());
        EmployeeCategoryMaster saved = repository.save(entity);
        log.info("EmployeeCategoryMasterServiceImpl:update :: id={} by={}", id, actor);
        return toResponse(saved);
    }

    @Override
    public MasterDataResponse findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Override
    public List<MasterDataResponse> findAll(boolean activeOnly) {
        List<EmployeeCategoryMaster> list = activeOnly ? repository.findByActiveTrue() : repository.findAll();
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    public EmployeeCategoryMaster getEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("EmployeeCategoryMaster", id));
    }

    @Override
    public EmployeeCategoryMaster getEntityByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeCategoryMaster not found with code: " + code));
    }

    private MasterDataResponse toResponse(EmployeeCategoryMaster e) {
        return MasterDataResponse.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName())
                .description(e.getDescription()).active(e.getActive()).build();
    }
}