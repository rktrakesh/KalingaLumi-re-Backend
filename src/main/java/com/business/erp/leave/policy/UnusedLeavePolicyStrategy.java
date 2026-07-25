package com.business.erp.leave.policy;

import com.business.erp.settings.enums.UnusedLeavePolicy;

import java.math.BigDecimal;

/**
 * One implementation per {@link UnusedLeavePolicy} value. Adding a new unused-leave
 * policy is purely additive: implement this interface, annotate with {@code @Component},
 * done — {@code LeaveSettlementServiceImpl} discovers it automatically via
 * {@code Map<UnusedLeavePolicy, UnusedLeavePolicyStrategy>} and never branches on the
 * policy type itself.
 */
public interface UnusedLeavePolicyStrategy {

    UnusedLeavePolicy policyType();

    UnusedLeaveOutcome apply(int unusedDays, int carryForwardLimit, BigDecimal dailySalary);
}