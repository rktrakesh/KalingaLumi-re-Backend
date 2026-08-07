package com.business.erp.employee.service.impl;

import com.business.erp.auth.service.AuthenticatedEmployeeAccessService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.storage.FileStorageService;
import com.business.erp.common.storage.StoredFile;
import com.business.erp.employee.config.EmployeeDocumentStorageProperties;
import com.business.erp.employee.dto.response.EmployeeDocumentContent;
import com.business.erp.employee.dto.response.EmployeeDocumentListResponse;
import com.business.erp.employee.dto.response.EmployeeDocumentResponse;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.entity.EmployeeDocument;
import com.business.erp.employee.enums.EmployeeDocumentStatus;
import com.business.erp.employee.enums.EmployeeDocumentType;
import com.business.erp.employee.repository.EmployeeDocumentRepository;
import com.business.erp.employee.repository.EmployeeRepository;
import com.business.erp.employee.service.EmployeeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService storageService;
    private final EmployeeDocumentStorageProperties storageProperties;
    private final AuthenticatedEmployeeAccessService accessService;
    private final AuditService auditService;
    private final ClockProvider clockProvider;

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocumentListResponse listCurrent(Long employeeId, UserDetails principal) {
        requireEmployee(employeeId);
        accessService.requireAdminHrOrEmployeeSelf(principal, employeeId);
        boolean canManage = accessService.isAdminOrHr(principal);
        List<EmployeeDocumentResponse> documents = documentRepository
                .findByEmployeeIdAndStatusOrderByDocumentTypeAscCreatedDateDesc(
                        employeeId, EmployeeDocumentStatus.CURRENT)
                .stream().map(document -> toResponse(document, canManage)).toList();
        return EmployeeDocumentListResponse.builder()
                .employeeId(employeeId)
                .canManage(canManage)
                .requiredDocumentTypes(Arrays.stream(EmployeeDocumentType.values())
                        .filter(EmployeeDocumentType::isRequiredForActivation).toList())
                .supportedDocumentTypes(List.of(EmployeeDocumentType.values()))
                .documents(documents)
                .build();
    }

    @Override
    @Transactional
    public EmployeeDocumentResponse upload(Long employeeId, EmployeeDocumentType type,
                                           LocalDate expiryDate, MultipartFile file,
                                           UserDetails principal) {
        accessService.requireAdminOrHr(principal);
        Employee employee = requireEmployee(employeeId);
        String actor = principal.getUsername();
        StoredFile stored = storageService.store(file, "employee-documents/" + employeeId,
                type.allowedFileTypes(), storageProperties.maxBytes(type));
        try {
            boolean replacement = false;
            List<Long> supersededDocumentIds = List.of();
            if (!type.allowsMultipleCurrent()) {
                List<EmployeeDocument> current = documentRepository.findCurrentForUpdate(
                        employeeId, type, EmployeeDocumentStatus.CURRENT);
                replacement = !current.isEmpty();
                supersededDocumentIds = current.stream().map(EmployeeDocument::getId).toList();
                current.forEach(existing -> {
                    existing.setStatus(EmployeeDocumentStatus.SUPERSEDED);
                    existing.setArchivedBy(actor);
                    existing.setArchivedAt(clockProvider.now());
                    existing.setUpdatedBy(actor);
                    existing.setUpdatedDate(clockProvider.now());
                });
                if (!current.isEmpty()) documentRepository.saveAllAndFlush(current);
            }

            EmployeeDocument document = EmployeeDocument.builder()
                    .employee(employee)
                    .documentType(type)
                    .originalFileName(stored.originalFilename())
                    .storageKey(stored.storageKey())
                    .mimeType(stored.contentType())
                    .fileSize(stored.size())
                    .expiryDate(expiryDate)
                    .status(EmployeeDocumentStatus.CURRENT)
                    .createdBy(actor)
                    .createdDate(clockProvider.now())
                    .build();
            EmployeeDocument saved = documentRepository.saveAndFlush(document);
            auditService.logAs(actor, "EMPLOYEE_DOCUMENT", replacement ? "REPLACE" : "UPLOAD", "EmployeeDocument",
                    saved.getId(), replacement ? Map.of("supersededDocumentIds", supersededDocumentIds) : null,
                    auditSummary(saved));
            return toResponse(saved, true);
        } catch (RuntimeException exception) {
            storageService.delete(stored.storageKey());
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocumentResponse getMetadata(Long employeeId, Long documentId, UserDetails principal) {
        EmployeeDocument document = load(documentId);
        requireDocumentEmployee(document, employeeId);
        authorizeView(document, principal);
        return toResponse(document, accessService.isAdminOrHr(principal));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocumentContent getContent(Long employeeId, Long documentId, UserDetails principal) {
        EmployeeDocument document = load(documentId);
        requireDocumentEmployee(document, employeeId);
        authorizeView(document, principal);
        auditService.logAs(principal.getUsername(), "EMPLOYEE_DOCUMENT", "DOWNLOAD",
                "EmployeeDocument", documentId, null, auditSummary(document));
        return content(document);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocumentContent getCurrentProfilePhoto(Long employeeId, UserDetails principal) {
        requireEmployee(employeeId);
        accessService.requireAdminHrManagerOrSelf(principal, employeeId);
        EmployeeDocument document = documentRepository
                .findFirstByEmployeeIdAndDocumentTypeAndStatusOrderByCreatedDateDesc(
                        employeeId, EmployeeDocumentType.PROFILE_PHOTO, EmployeeDocumentStatus.CURRENT)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile photo not found"));
        return content(document);
    }

    @Override
    @Transactional
    public void archive(Long employeeId, Long documentId, UserDetails principal) {
        accessService.requireAdminOrHr(principal);
        EmployeeDocument document = documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDocument", documentId));
        requireDocumentEmployee(document, employeeId);
        if (document.getStatus() != EmployeeDocumentStatus.CURRENT) {
            throw new BusinessException("Only a current employee document can be archived");
        }
        String actor = principal.getUsername();
        document.setStatus(EmployeeDocumentStatus.ARCHIVED);
        document.setArchivedBy(actor);
        document.setArchivedAt(clockProvider.now());
        document.setUpdatedBy(actor);
        document.setUpdatedDate(clockProvider.now());
        documentRepository.save(document);
        auditService.logAs(actor, "EMPLOYEE_DOCUMENT", "ARCHIVE", "EmployeeDocument",
                documentId, null, auditSummary(document));
    }

    private void authorizeView(EmployeeDocument document, UserDetails principal) {
        if (document.getStatus() != EmployeeDocumentStatus.CURRENT
                && !accessService.isAdminOrHr(principal)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Archived employee documents are restricted");
        }
        if (document.getDocumentType() == EmployeeDocumentType.PROFILE_PHOTO) {
            accessService.requireAdminHrManagerOrSelf(principal, document.getEmployee().getId());
        } else {
            accessService.requireAdminHrOrEmployeeSelf(principal, document.getEmployee().getId());
        }
    }

    private Employee requireEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    private EmployeeDocument load(Long documentId) {
        return documentRepository.findByIdWithEmployee(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDocument", documentId));
    }

    private void requireDocumentEmployee(EmployeeDocument document, Long employeeId) {
        if (!document.getEmployee().getId().equals(employeeId)) {
            throw new ResourceNotFoundException("Employee document not found");
        }
    }

    private EmployeeDocumentContent content(EmployeeDocument document) {
        return new EmployeeDocumentContent(storageService.load(document.getStorageKey()),
                document.getMimeType(), document.getOriginalFileName());
    }

    private EmployeeDocumentResponse toResponse(EmployeeDocument document, boolean canManage) {
        return EmployeeDocumentResponse.builder()
                .id(document.getId())
                .employeeId(document.getEmployee().getId())
                .documentType(document.getDocumentType())
                .originalFileName(document.getOriginalFileName())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .expiryDate(document.getExpiryDate())
                .status(document.getStatus())
                .uploadedBy(document.getCreatedBy())
                .uploadedAt(document.getCreatedDate())
                .required(document.getDocumentType().isRequiredForActivation())
                .canManage(canManage)
                .canView(true)
                .canDownload(true)
                .build();
    }

    private Map<String, Object> auditSummary(EmployeeDocument document) {
        return Map.of(
                "employeeId", document.getEmployee().getId(),
                "documentType", document.getDocumentType(),
                "fileName", document.getOriginalFileName(),
                "status", document.getStatus());
    }
}
