package com.business.erp.notification.service.impl;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.employee.entity.Employee;
import com.business.erp.notification.dto.NotificationResponse;
import com.business.erp.notification.entity.Notification;
import com.business.erp.notification.repository.NotificationRepository;
import com.business.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createForgottenCheckoutNotification(Employee emp, LocalDate date) {
        log.info("NotificationServiceImpl:createForgottenCheckoutNotification :: emp={} date={}", emp.getEmployeeCode(), date);
        notifyAdmins(Notification.NotificationType.FORGOTTEN_CHECKOUT, "Forgotten Checkout",
                emp.getName() + " (" + emp.getEmployeeCode() + ") did not check out on " + date,
                "AttendanceRecord", null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOvertimeApprovalNotification(Employee emp, Long otId) {
        log.info("NotificationServiceImpl:createOvertimeApprovalNotification :: emp={} otId={}", emp.getEmployeeCode(), otId);
        notifyAdmins(Notification.NotificationType.PENDING_OT_APPROVAL, "Overtime Approval Pending",
                emp.getName() + " has a pending overtime request", "OvertimeRequest", otId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createLeaveApprovalNotification(Employee emp, Long leaveId) {
        log.info("NotificationServiceImpl:createLeaveApprovalNotification :: emp={} leaveId={}", emp.getEmployeeCode(), leaveId);
        notifyAdmins(Notification.NotificationType.PENDING_LEAVE_APPROVAL, "Leave Approval Pending",
                emp.getName() + " has applied for leave on " + leaveId, "LeaveRequest", leaveId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createLoanApprovalNotification(Employee emp, Long loanId) {
        log.info("NotificationServiceImpl:createLoanApprovalNotification :: emp={} loanId={}", emp.getEmployeeCode(), loanId);
        notifyAdmins(Notification.NotificationType.PENDING_LOAN_APPROVAL, "Loan Approval Pending",
                "Loan request for " + emp.getName() + " is pending approval", "EmployeeLoan", loanId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createLowInventoryNotification(String materialName, Long materialId) {
        log.warn("NotificationServiceImpl:createLowInventoryNotification :: material={} id={}", materialName, materialId);
        notifyAdminsAndManagers(Notification.NotificationType.LOW_INVENTORY, "Low Inventory Alert",
                "Material '" + materialName + "' has reached reorder level", "Material", materialId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPayrollPendingNotification(int year, int month) {
        log.info("NotificationServiceImpl:createPayrollPendingNotification :: {}/{}", year, month);
        notifyAdmins(Notification.NotificationType.PAYROLL_PENDING, "Payroll Generation Pending",
                "Payroll for " + year + "-" + String.format("%02d", month) + " has not been generated", null, null);
    }

    @Override
    public List<NotificationResponse> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, Notification.NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public void markRead(Long notifId) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            n.setStatus(Notification.NotificationStatus.READ);
            n.setReadDate(LocalDateTime.now());
            notificationRepository.save(n);
            log.debug("NotificationServiceImpl:markRead :: notifId={}", notifId);
        });
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUser(userId);
        log.debug("NotificationServiceImpl:markAllRead :: userId={}", userId);
    }

    private void notifyAdmins(Notification.NotificationType type, String title, String message,
                              String refType, Long refId) {
        userRepository.findByStatusAndRoleIn(User.UserStatus.ACTIVE,
                List.of(User.Role.ROLE_ADMIN)).forEach(u ->
                notificationRepository.save(Notification.builder()
                        .user(u).notificationType(type).title(title).message(message)
                        .referenceType(refType).referenceId(refId).build()));
    }

    private void notifyAdminsAndManagers(Notification.NotificationType type, String title,
                                         String message, String refType, Long refId) {
        userRepository.findByStatusAndRoleIn(User.UserStatus.ACTIVE,
                List.of(User.Role.ROLE_ADMIN, User.Role.ROLE_MANAGER)).forEach(u ->
                notificationRepository.save(Notification.builder()
                        .user(u).notificationType(type).title(title).message(message)
                        .referenceType(refType).referenceId(refId).build()));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).notificationType(n.getNotificationType().name())
                .title(n.getTitle()).message(n.getMessage())
                .referenceType(n.getReferenceType()).referenceId(n.getReferenceId())
                .status(n.getStatus().name()).createdDate(n.getCreatedDate()).readDate(n.getReadDate()).build();
    }
}
