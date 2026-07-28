package com.business.erp.leave.policy;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** What happened to an employee's unused paid-leave balance under one {@link UnusedLeavePolicyStrategy}. */
@Value
@Builder
public class UnusedLeaveOutcome {
    int expiredDays;
    int encashedDays;
    BigDecimal encashmentAmount;
    int carriedForwardDays;
}