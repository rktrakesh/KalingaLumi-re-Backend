package com.business.erp.performance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerOwnershipResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long employeeId;
    private String employeeName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isTemporary;
    private String status;
    private String remarks;
}