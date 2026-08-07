package com.business.erp.employee.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMasterDataRequest {
    @Size(max = 100)
    private String name;
    @Size(max = 255)
    private String description;
    private Boolean active;
}