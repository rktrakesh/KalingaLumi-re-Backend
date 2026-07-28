package com.business.erp.leave.policy;

import com.business.erp.settings.enums.UnusedLeavePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Unused paid leave is converted into additional salary: Unused Days x Daily Salary. */
@Component
public class EncashLeavePolicy implements UnusedLeavePolicyStrategy {

    @Override
    public UnusedLeavePolicy policyType() {
        return UnusedLeavePolicy.ENCASH;
    }

    @Override
    public UnusedLeaveOutcome apply(int unusedDays, int carryForwardLimit, BigDecimal dailySalary) {
        BigDecimal amount = dailySalary.multiply(BigDecimal.valueOf(unusedDays)).setScale(2, RoundingMode.HALF_UP);
        return UnusedLeaveOutcome.builder()
                .expiredDays(0).encashedDays(unusedDays).encashmentAmount(amount).carriedForwardDays(0)
                .build();
    }
}