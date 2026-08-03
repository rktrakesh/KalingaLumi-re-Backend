package com.business.erp.employee.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDesignationRequest {
    @Size(max = 100)
    private String name;
    private Long categoryId;
    @Size(max = 255)
    private String description;
    private Boolean active;
}