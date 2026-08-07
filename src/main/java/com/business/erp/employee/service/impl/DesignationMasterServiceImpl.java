package com.business.erp.employee.service.impl;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.dto.request.CreateDesignationRequest;
import com.business.erp.employee.dto.request.UpdateDesignationRequest;
import com.business.erp.employee.dto.response.DesignationResponse;
import com.business.erp.employee.entity.DesignationMaster;
import com.business.erp.employee.entity.EmployeeCategoryMaster;
import com.business.erp.employee.repository.DesignationMasterRepository;
import com.business.erp.employee.repository.EmployeeCategoryMasterRepository;
import com.business.erp.employee.service.DesignationMasterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignationMasterServiceImpl implements DesignationMasterService {

    private final DesignationMasterRepository repository;
    private final EmployeeCategoryMasterRepository categoryRepository;
    private final Logger log = LoggerFactory.getLogger(DesignationMasterServiceImpl.class);

    @Override
    @Transactional
    public DesignationResponse create(CreateDesignationRequest request, String actor) {
        if (repository.existsByCode(request.getCode())) {
            throw new BusinessException("DUPLICATE_CODE: a designation with code '" + request.getCode() + "' already exists");
        }
        EmployeeCategoryMaster category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeCategoryMaster", request.getCategoryId()));

        DesignationMaster entity = DesignationMaster.builder()
                .code(request.getCode()).name(request.getName()).category(category)
                .description(request.getDescription()).active(true).build();
        entity.setCreatedBy(actor);
        entity.setCreatedDate(LocalDateTime.now());
        DesignationMaster saved = repository.save(entity);
        log.info("DesignationMasterServiceImpl:create :: code={} categoryId={} by={}", request.getCode(), request.getCategoryId(), actor);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DesignationResponse update(Long id, UpdateDesignationRequest request, String actor) {
        DesignationMaster entity = getEntityById(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getActive() != null) entity.setActive(request.getActive());
        if (request.getCategoryId() != null) {
            EmployeeCategoryMaster category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("EmployeeCategoryMaster", request.getCategoryId()));
            entity.setCategory(category);
        }
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(LocalDateTime.now());
        DesignationMaster saved = repository.save(entity);
        log.info("DesignationMasterServiceImpl:update :: id={} by={}", id, actor);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationResponse findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationResponse> findAll(boolean activeOnly) {
        List<DesignationMaster> list = activeOnly ? repository.findByActiveTrue() : repository.findAll();
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationResponse> findByCategory(Long categoryId) {
        return repository.findByCategoryIdAndActiveTrue(categoryId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationMaster getEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("DesignationMaster", id));
    }

    private DesignationResponse toResponse(DesignationMaster d) {
        return DesignationResponse.builder()
                .id(d.getId()).code(d.getCode()).name(d.getName())
                .categoryId(d.getCategory().getId()).categoryCode(d.getCategory().getCode()).categoryName(d.getCategory().getName())
                .description(d.getDescription()).active(d.getActive()).build();
    }
}