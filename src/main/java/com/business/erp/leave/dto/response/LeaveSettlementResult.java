package com.business.erp.leave.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LeaveSettlementResult {
    private int unusedDays;
    private String policyApplied;
    private int expiredDays;
    private int encashedDays;
    private BigDecimal encashmentAmount;
    private int carriedForwardDays;
}