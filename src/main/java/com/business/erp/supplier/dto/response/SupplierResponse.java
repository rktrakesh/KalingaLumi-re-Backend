package com.business.erp.supplier.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SupplierResponse {
    private Long id;
    private String supplierCode;
    private String name;
    private String phone;
    private String address;
    private String materialsSupplied;
    private String status;
    private BigDecimal outstandingPayable;
}
