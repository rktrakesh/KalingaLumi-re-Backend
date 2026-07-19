package com.business.erp.notification.service;

import com.business.erp.employee.entity.Employee;
import com.business.erp.notification.dto.NotificationResponse;

import java.time.LocalDate;
import java.util.List;

public interface NotificationService {
    void createForgottenCheckoutNotification(Employee emp, LocalDate date);

    void createOvertimeApprovalNotification(Employee emp, Long otId);

    void createLeaveApprovalNotification(Employee emp, Long leaveId);

    void createLoanApprovalNotification(Employee emp, Long loanId);

    void createLowInventoryNotification(String materialName, Long materialId);

    void createPayrollPendingNotification(int year, int month);

    List<NotificationResponse> getForUser(Long userId);

    long getUnreadCount(Long userId);

    void markRead(Long notifId);

    void markAllRead(Long userId);
}
