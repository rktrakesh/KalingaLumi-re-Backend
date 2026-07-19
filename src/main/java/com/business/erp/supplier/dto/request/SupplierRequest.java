package com.business.erp.supplier.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierRequest {
    @NotBlank
    private String name;
    private String phone;
    private String address;
    private String materialsSupplied;
}
