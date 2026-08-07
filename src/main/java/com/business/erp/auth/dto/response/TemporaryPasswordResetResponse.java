package com.business.erp.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Plain temporary password is returned once to the authorized administrator and is never persisted. */
@Data
@Builder
public class TemporaryPasswordResetResponse {
    private String temporaryPassword;
    private LocalDateTime temporaryPasswordExpiresAt;
    private Boolean mustChangePassword;
    private Boolean emailDeliveryAttempted;
}
