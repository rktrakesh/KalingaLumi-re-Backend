package com.business.erp.employee.dto.response;

import com.business.erp.employee.enums.EmployeeDocumentType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmployeeDocumentListResponse {
    private Long employeeId;
    private boolean canManage;
    private List<EmployeeDocumentType> requiredDocumentTypes;
    private List<EmployeeDocumentType> supportedDocumentTypes;
    private List<EmployeeDocumentResponse> documents;
}
