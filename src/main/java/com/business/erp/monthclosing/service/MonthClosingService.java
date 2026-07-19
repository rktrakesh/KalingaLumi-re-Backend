package com.business.erp.monthclosing.service;

import com.business.erp.monthclosing.dto.response.MonthClosingResponse;
import com.business.erp.monthclosing.dto.response.PreCloseCheckResponse;

import java.util.List;

public interface MonthClosingService {
    boolean isMonthClosed(int year, int month);

    PreCloseCheckResponse preCheck(int year, int month);

    MonthClosingResponse closeMonth(int year, int month, String closedBy);

    MonthClosingResponse reopenMonth(int year, int month, String remarks, String reopenedBy);

    MonthClosingResponse getStatus(int year, int month);

    List<MonthClosingResponse> getHistory();
}
