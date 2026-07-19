package com.business.erp.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ProfitLossResponse {
    private int year;
    private int month;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private Map<String, BigDecimal> expenseByCategory;
    private BigDecimal grossProfit;
    private BigDecimal netProfit;
}
