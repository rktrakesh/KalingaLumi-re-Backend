package com.business.erp.monthclosing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreCloseCheckResponse {
    private boolean canClose;
    private long pendingCheckouts;
    private long pendingOvertime;
    private boolean payrollGenerated;
    private List<String> blockers;
}
