package com.business.erp.payroll.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DisbursePaymentRequest {

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String remarks;

    public enum PaymentMode {CASH, BANK}
}