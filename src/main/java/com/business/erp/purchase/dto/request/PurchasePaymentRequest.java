package com.business.erp.purchase.dto.request;

import com.business.erp.purchase.entity.PurchasePayment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchasePaymentRequest {
    @NotNull
    private LocalDate paymentDate;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private PurchasePayment.PaymentMode paymentMode;
    private String remarks;
}
