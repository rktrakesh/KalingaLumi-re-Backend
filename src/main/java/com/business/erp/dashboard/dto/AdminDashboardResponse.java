package com.business.erp.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    // Attendance
    private long presentToday;
    private long absentToday;
    private long pendingCheckouts;

    // Approvals
    private long pendingOvertimeApprovals;
    private long pendingLoanApprovals;
    private long pendingLeaveApprovals;

    // Inventory
    private List<StockItem> rawMaterialStock;
    private List<StockItem> finishedGoodsStock;
    private long lowStockAlerts;

    // Finance
    private BigDecimal customerOutstanding;
    private BigDecimal supplierOutstanding;
    private BigDecimal monthlyRevenue;
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlyProfit;
    private BigDecimal outstandingLoanBalance;
    private BigDecimal cashInHand;
    private BigDecimal bankBalance;

    @Data
    @Builder
    public static class StockItem {
        private String materialName;
        private BigDecimal currentStock;
        private String unit;
    }
}
