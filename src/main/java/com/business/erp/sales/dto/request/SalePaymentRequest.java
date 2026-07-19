package com.business.erp.sales.dto.request;

import com.business.erp.sales.entity.SalesPayment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalePaymentRequest {
    @NotNull
    private LocalDate paymentDate;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private SalesPayment.PaymentMode paymentMode;
    private String remarks;
}
