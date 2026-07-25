package com.business.erp.leave.service;

import com.business.erp.leave.dto.response.LeaveSettlementResult;
import com.business.erp.settings.enums.UnusedLeavePolicy;

import java.math.BigDecimal;

/**
 * Applies the UNUSED_PAID_LEAVE_POLICY (as captured in a payroll settings snapshot,
 * never live settings) to one employee's leave balance at month end. Invoked by the
 * payroll engine when a run transitions to APPROVED — never called ad-hoc, so every
 * month is settled at most once (enforced by the unique employee+year+month constraint
 * on leave_settlement_logs).
 */
public interface LeaveSettlementService {

    /** Returns the employee's remaining paid-leave balance for the month (creates the balance row if absent). */
    int getRemainingBalance(Long employeeId, int year, int month);

    /**
     * Consumes up to {@code requestedDays} of automatic paid leave from the employee's balance for the month.
     * Returns the number of days actually consumed (may be less than requested if balance is insufficient).
     */
    int consumeAutomaticLeave(Long employeeId, int year, int month, int requestedDays);

    /** Reverses a previous consumeAutomaticLeave call — used when a payroll version is superseded before recompute. */
    void restoreAutomaticLeave(Long employeeId, int year, int month, int days);

    /**
     * Settles the employee's remaining unused balance for (year, month) according to the given policy,
     * persists an immutable {@link com.business.erp.leave.entity.LeaveSettlementLog}, and — for
     * CARRY_FORWARD — credits the (capped) carried-forward days onto next month's balance.
     * Idempotent: if a settlement already exists for this employee+month, it is returned unchanged
     * rather than re-applied (payroll reopen/recalculate must explicitly reverse first).
     */
    LeaveSettlementResult settleUnusedLeave(Long payrollRunId, Long employeeId, int year, int month,
                                            UnusedLeavePolicy policy, int carryForwardLimit,
                                            boolean encashmentEnabled, BigDecimal dailySalary, String settledBy);

    /** Reverses a previously-applied settlement (used when a payroll run is reopened before recalculation). */
    void reverseSettlement(Long employeeId, int year, int month);
}