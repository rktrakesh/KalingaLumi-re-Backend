package com.business.erp.leave.policy;

import com.business.erp.settings.enums.UnusedLeavePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Unused paid leave simply becomes zero at month end. */
@Component
public class ExpireLeavePolicy implements UnusedLeavePolicyStrategy {

    @Override
    public UnusedLeavePolicy policyType() {
        return UnusedLeavePolicy.EXPIRE;
    }

    @Override
    public UnusedLeaveOutcome apply(int unusedDays, int carryForwardLimit, BigDecimal dailySalary) {
        return UnusedLeaveOutcome.builder()
                .expiredDays(unusedDays).encashedDays(0).encashmentAmount(BigDecimal.ZERO).carriedForwardDays(0)
                .build();
    }
}