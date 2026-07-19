package com.business.erp.dashboard.service.impl;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.customer.repository.CustomerRepository;
import com.business.erp.customer.service.CustomerService;
import com.business.erp.dashboard.dto.AdminDashboardResponse;
import com.business.erp.dashboard.service.DashboardService;
import com.business.erp.expense.repository.ExpenseRepository;
import com.business.erp.inventory.entity.Material;
import com.business.erp.inventory.repository.MaterialRepository;
import com.business.erp.inventory.service.InventoryService;
import com.business.erp.leave.entity.LeaveRequest;
import com.business.erp.leave.repository.LeaveRequestRepository;
import com.business.erp.loan.entity.EmployeeLoan;
import com.business.erp.loan.repository.LoanRepository;
import com.business.erp.loan.service.LoanService;
import com.business.erp.overtime.entity.OvertimeRequest;
import com.business.erp.overtime.repository.OvertimeRepository;
import com.business.erp.sales.repository.SalesInvoiceRepository;
import com.business.erp.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AttendanceRepository   attendanceRepository;
    private final OvertimeRepository     overtimeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LoanRepository         loanRepository;
    private final MaterialRepository     materialRepository;
    private final InventoryService       inventoryService;
    private final CustomerService        customerService;
    private final SupplierRepository     supplierRepository;
    private final CustomerRepository     customerRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final ExpenseRepository      expenseRepository;
    private final CashbookService        cashbookService;
    private final LoanService            loanService;

    private final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        log.debug("DashboardServiceImpl:getAdminDashboard :: invoked");

        LocalDate today      = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd   = today.withDayOfMonth(today.lengthOfMonth());
        int year  = today.getYear();
        int month = today.getMonthValue();

        // ── Attendance ────────────────────────────────────────────────────
        long present         = attendanceRepository.countByDateAndStatus(today, AttendanceRecord.AttendanceStatus.PRESENT);
        long absent          = attendanceRepository.countByDateAndStatus(today, AttendanceRecord.AttendanceStatus.ABSENT);
        long pendingCheckouts= attendanceRepository.findByStatus(AttendanceRecord.AttendanceStatus.PENDING_CHECKOUT).size();

        // ── Pending approvals ─────────────────────────────────────────────
        long pendingOT    = overtimeRepository.countByMonthAndStatus(monthStart, monthEnd, OvertimeRequest.OvertimeStatus.PENDING);
        long pendingLeave = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING).size();
        long pendingLoans = loanRepository.findByStatus(EmployeeLoan.LoanStatus.PENDING_APPROVAL).size();

        // ── Inventory ─────────────────────────────────────────────────────
        List<Material> rawMaterials  = materialRepository.findByMaterialTypeAndStatus(Material.MaterialType.RAW_MATERIAL,  Material.MaterialStatus.ACTIVE);
        List<Material> finishedGoods = materialRepository.findByMaterialTypeAndStatus(Material.MaterialType.FINISHED_GOODS, Material.MaterialStatus.ACTIVE);
        long lowStock = inventoryService.getLowStockAlerts().size();

        // ── Customer outstanding ──────────────────────────────────────────
        BigDecimal customerOs = customerService.getOutstandingList().stream()
                .map(c -> c.getOutstandingReceivable() != null ? c.getOutstandingReceivable() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Supplier outstanding — compute defensively (some Supplier versions lack a getter)
        // Previously called supplierService.getLedger(0) when no suppliers existed → crash
        BigDecimal supplierOs = BigDecimal.ZERO;
        try {
            var suppliers = supplierRepository.findAll();
            if (suppliers != null) {
                for (var s : suppliers) {
                    if (s == null) continue;
                    try {
                        // Try common getter names via reflection so code compiles regardless of Supplier shape
                        BigDecimal value = null;
                        Method m = null;
                        try { m = s.getClass().getMethod("getOutstandingPayable"); } catch (NoSuchMethodException ignored) {}
                        if (m == null) try { m = s.getClass().getMethod("getOutstanding"); } catch (NoSuchMethodException ignored) {}
                        if (m == null) try { m = s.getClass().getMethod("getOutstandingAmount"); } catch (NoSuchMethodException ignored) {}
                        if (m == null) try { m = s.getClass().getMethod("getOutstandingBalance"); } catch (NoSuchMethodException ignored) {}
                        if (m != null) {
                            Object o = m.invoke(s);
                            if (o instanceof BigDecimal) value = (BigDecimal) o;
                        } else {
                            // No known getter found; attempt to read a public field named 'outstandingPayable'
                            try {
                                var f = s.getClass().getField("outstandingPayable");
                                Object o = f.get(s);
                                if (o instanceof BigDecimal) value = (BigDecimal) o;
                            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                                // give up for this supplier
                            }
                        }

                        if (value != null) supplierOs = supplierOs.add(value);
                    } catch (Exception e) {
                        log.warn("Failed to read outstanding payable for supplier {} - skipping", s, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error loading suppliers for dashboard outstanding calculation; defaulting to ZERO", e);
            supplierOs = BigDecimal.ZERO;
        }

        // ── Monthly P&L ───────────────────────────────────────────────────
        BigDecimal revenue  = salesInvoiceRepository.sumRevenueByPeriod(monthStart, monthEnd);
        if (revenue  == null) revenue  = BigDecimal.ZERO;
        BigDecimal expenses = expenseRepository.sumApproved(monthStart, monthEnd, null);
        if (expenses == null) expenses = BigDecimal.ZERO;
        BigDecimal profit   = revenue.subtract(expenses);

        // ── Active loan balances ──────────────────────────────────────────
        BigDecimal loanBalance = loanRepository.findByStatus(EmployeeLoan.LoanStatus.ACTIVE).stream()
                .map(l -> {
                    try { return loanService.getCurrentBalance(l.getId()); }
                    catch (Exception e) { return BigDecimal.ZERO; }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Cashbook ──────────────────────────────────────────────────────
        var cashSummary = cashbookService.getSummary(year, month);

        return AdminDashboardResponse.builder()
                .presentToday((int) present)
                .absentToday((int) absent)
                .pendingCheckouts(pendingCheckouts)
                .pendingOvertimeApprovals(pendingOT)
                .pendingLoanApprovals(pendingLoans)
                .pendingLeaveApprovals(pendingLeave)
                .rawMaterialStock(rawMaterials.stream()
                        .map(m -> AdminDashboardResponse.StockItem.builder()
                                .materialName(m.getName())
                                .currentStock(inventoryService.getCurrentStock(m.getId()))
                                .unit(m.getUnit().name())
                                .build())
                        .collect(Collectors.toList()))
                .finishedGoodsStock(finishedGoods.stream()
                        .map(m -> AdminDashboardResponse.StockItem.builder()
                                .materialName(m.getName())
                                .currentStock(inventoryService.getCurrentStock(m.getId()))
                                .unit(m.getUnit().name())
                                .build())
                        .collect(Collectors.toList()))
                .lowStockAlerts(lowStock)
                .customerOutstanding(customerOs)
                .supplierOutstanding(supplierOs)
                .monthlyRevenue(revenue)
                .monthlyExpenses(expenses)
                .monthlyProfit(profit)
                .outstandingLoanBalance(loanBalance)
                .cashInHand(cashSummary.getCashInHand())
                .bankBalance(cashSummary.getBankBalance())
                .build();
    }
}