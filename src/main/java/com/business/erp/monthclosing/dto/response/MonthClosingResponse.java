package com.business.erp.monthclosing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonthClosingResponse {
    private Long id;
    private int year;
    private int month;
    private String status;
    private String closedBy;
    private LocalDateTime closedDate;
    private String reopenedBy;
    private LocalDateTime reopenedDate;
    private String reopenRemarks;
}
