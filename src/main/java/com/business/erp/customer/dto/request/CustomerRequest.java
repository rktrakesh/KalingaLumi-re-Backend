package com.business.erp.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank
    private String name;
    private String phone;
    private String address;
    private String gstNumber;
    private Integer creditDays;
}
