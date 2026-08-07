package com.business.erp.employee.service;

import com.business.erp.employee.dto.response.EmployeeDocumentContent;
import com.business.erp.employee.dto.response.EmployeeDocumentListResponse;
import com.business.erp.employee.dto.response.EmployeeDocumentResponse;
import com.business.erp.employee.enums.EmployeeDocumentType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface EmployeeDocumentService {
    EmployeeDocumentListResponse listCurrent(Long employeeId, UserDetails principal);

    EmployeeDocumentResponse upload(Long employeeId, EmployeeDocumentType type,
                                    LocalDate expiryDate, MultipartFile file, UserDetails principal);

    EmployeeDocumentResponse getMetadata(Long employeeId, Long documentId, UserDetails principal);

    EmployeeDocumentContent getContent(Long employeeId, Long documentId, UserDetails principal);

    EmployeeDocumentContent getCurrentProfilePhoto(Long employeeId, UserDetails principal);

    void archive(Long employeeId, Long documentId, UserDetails principal);
}
