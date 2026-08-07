package com.business.erp.employee.dto.response;

import com.business.erp.employee.enums.EmployeeDocumentStatus;
import com.business.erp.employee.enums.EmployeeDocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeDocumentResponse {
    private Long id;
    private Long employeeId;
    private EmployeeDocumentType documentType;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private LocalDate expiryDate;
    private EmployeeDocumentStatus status;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private boolean required;
    private boolean canManage;
    private boolean canView;
    private boolean canDownload;
}
