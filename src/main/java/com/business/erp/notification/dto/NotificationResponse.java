package com.business.erp.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String notificationType;
    private String title;
    private String message;
    private String referenceType;
    private Long referenceId;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime readDate;
}
