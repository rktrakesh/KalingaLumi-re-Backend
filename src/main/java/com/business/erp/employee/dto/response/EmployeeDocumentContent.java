package com.business.erp.employee.dto.response;

import org.springframework.core.io.Resource;

public record EmployeeDocumentContent(Resource resource, String contentType, String filename) {
}
