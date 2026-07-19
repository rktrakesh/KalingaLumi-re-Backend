package com.business.erp.attendance.scheduler;

import com.business.erp.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceService attendanceService;
    private final Logger log = LoggerFactory.getLogger(AttendanceScheduler.class);

    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Kolkata")
    public void markForgottenCheckouts() {
        log.info("AttendanceScheduler:markForgottenCheckouts :: Running forgotten checkout detection job");
        try {
            attendanceService.markPendingCheckouts();
            log.info("AttendanceScheduler:markForgottenCheckouts :: Job completed successfully");
        } catch (Exception e) {
            log.error("AttendanceScheduler:markForgottenCheckouts :: Job failed :: {}", e.getMessage(), e);
        }
    }
}
