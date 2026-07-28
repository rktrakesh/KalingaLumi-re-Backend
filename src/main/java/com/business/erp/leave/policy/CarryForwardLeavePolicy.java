package com.business.erp.leave.policy;

import com.business.erp.settings.enums.UnusedLeavePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Unused paid leave carries to next month, capped at the configured carry-forward limit; any excess expires. */
@Component
public class CarryForwardLeavePolicy implements UnusedLeavePolicyStrategy {

    @Override
    public UnusedLeavePolicy policyType() {
        return UnusedLeavePolicy.CARRY_FORWARD;
    }

    @Override
    public UnusedLeaveOutcome apply(int unusedDays, int carryForwardLimit, BigDecimal dailySalary) {
        int carried = Math.min(unusedDays, carryForwardLimit);
        int expired = unusedDays - carried;
        return UnusedLeaveOutcome.builder()
                .expiredDays(expired).encashedDays(0).encashmentAmount(BigDecimal.ZERO).carriedForwardDays(carried)
                .build();
    }
}