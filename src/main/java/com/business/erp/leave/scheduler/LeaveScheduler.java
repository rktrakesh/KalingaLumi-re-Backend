package com.business.erp.leave.scheduler;

import com.business.erp.leave.service.LeaveService;
import com.business.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class LeaveScheduler {

    private final LeaveService leaveService;
    private final NotificationService notificationService;
    private final Logger log =  LoggerFactory.getLogger(LeaveScheduler.class);

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Kolkata")
    public void allocateMonthlyLeaves() {
        LocalDate now = LocalDate.now();
        log.info("LeaveScheduler:allocateMonthlyLeaves :: Starting monthly leave allocation for {}/{}", now.getYear(), now.getMonthValue());
        try {
            leaveService.allocateMonthlyLeaves(now.getYear(), now.getMonthValue());
            log.info("LeaveScheduler:allocateMonthlyLeaves :: SUCCESS {}/{}", now.getYear(), now.getMonthValue());
        } catch (Exception e) {
            log.error("LeaveScheduler:allocateMonthlyLeaves :: FAILED {}/{} :: {}", now.getYear(), now.getMonthValue(), e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 9 25 * *", zone = "Asia/Kolkata")
    public void payrollReminder() {
        LocalDate now = LocalDate.now();
        log.info("LeaveScheduler:payrollReminder :: Sending payroll pending reminder for {}/{}", now.getYear(), now.getMonthValue());
        try {
            notificationService.createPayrollPendingNotification(now.getYear(), now.getMonthValue());
            log.info("LeaveScheduler:payrollReminder :: SUCCESS");
        } catch (Exception e) {
            log.error("LeaveScheduler:payrollReminder :: FAILED :: {}", e.getMessage(), e);
        }
    }
}
