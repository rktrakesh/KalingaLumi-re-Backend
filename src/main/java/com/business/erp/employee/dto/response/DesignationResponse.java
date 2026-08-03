package com.business.erp.employee.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DesignationResponse {
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Boolean active;
}