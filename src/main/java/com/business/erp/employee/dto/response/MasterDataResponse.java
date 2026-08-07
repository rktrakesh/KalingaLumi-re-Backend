package com.business.erp.employee.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MasterDataResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;
}