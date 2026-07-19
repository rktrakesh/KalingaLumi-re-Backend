package com.business.erp.leave.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaveBalanceResponse {
    private Long employeeId;
    private String employeeName;
    private int year;
    private int month;
    private int allocated;
    private int used;
    private int balance;
}
