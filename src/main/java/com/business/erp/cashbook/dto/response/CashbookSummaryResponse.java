package com.business.erp.cashbook.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CashbookSummaryResponse {
    private BigDecimal cashInHand;
    private BigDecimal bankBalance;
    private BigDecimal totalBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal monthlyNet;
}
