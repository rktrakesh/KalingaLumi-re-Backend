package com.business.erp.customer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerResponse {
    private Long id;
    private String customerCode;
    private String name;
    private String phone;
    private String address;
    private String gstNumber;
    private Integer creditDays;
    private String status;
    private BigDecimal outstandingReceivable;
}
