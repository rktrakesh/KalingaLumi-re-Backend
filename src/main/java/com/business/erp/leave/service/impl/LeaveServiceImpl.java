package com.business.erp.leave.service.impl;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.holiday.service.HolidayService;
import com.business.erp.leave.dto.request.LeaveRequestDto;
import com.business.erp.leave.dto.response.LeaveBalanceResponse;
import com.business.erp.leave.dto.response.LeaveResponse;
import com.business.erp.leave.entity.LeaveBalance;
import com.business.erp.leave.entity.LeaveRequest;
import com.business.erp.leave.repository.LeaveBalanceRepository;
import com.business.erp.leave.repository.LeaveRequestRepository;
import com.business.erp.leave.service.LeaveService;
import com.business.erp.notification.service.NotificationService;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeService employeeService;
    private final AttendanceRepository attendanceRepository;
    private final HolidayService holidayService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final Logger log = LoggerFactory.getLogger(LeaveServiceImpl.class);

    @Override
    @Transactional
    public LeaveResponse createRequest(LeaveRequestDto req, String createdBy) {
        log.info("LeaveServiceImpl:createRequest :: empId={} date={} by={}", req.getEmployeeId(), req.getLeaveDate(), createdBy);
        Employee emp = employeeService.getEmployee(req.getEmployeeId());
        if (holidayService.isHoliday(req.getLeaveDate()))
            throw new BusinessException("Cannot apply leave on a holiday: " + req.getLeaveDate());
        if (leaveRequestRepository.existsByEmployeeIdAndLeaveDateAndStatusIn(
                req.getEmployeeId(), req.getLeaveDate(),
                List.of(LeaveRequest.LeaveStatus.PENDING, LeaveRequest.LeaveStatus.APPROVED)))
            throw new BusinessException("Leave request already exists for this date");
        LeaveRequest lr = leaveRequestRepository.save(LeaveRequest.builder()
                .employee(emp).leaveDate(req.getLeaveDate()).reason(req.getReason()).build());
        notificationService.createLeaveApprovalNotification(emp, lr.getId());
        log.info("LeaveServiceImpl:createRequest :: SUCCESS id={}", lr.getId());
        return toResponse(lr);
    }

    @Override
    @Transactional
    public LeaveResponse approve(Long id, String approvedBy) {
        log.info("LeaveServiceImpl:approve :: id={} by={}", id, approvedBy);
        LeaveRequest lr = getRequest(id);
        if (lr.getStatus() != LeaveRequest.LeaveStatus.PENDING)
            throw new BusinessException("Only PENDING leave requests can be approved");
        LeaveBalance balance = getOrCreateBalance(lr.getEmployee().getId(),
                lr.getLeaveDate().getYear(), lr.getLeaveDate().getMonthValue());
        if (balance.getBalance() <= 0)
            throw new BusinessException("No paid leave balance available for " + lr.getEmployee().getName());
        balance.useLeave();
        leaveBalanceRepository.save(balance);
        lr.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        lr.setApprovedBy(approvedBy);
        lr.setApprovedDate(LocalDateTime.now());
        attendanceRepository.findByEmployeeIdAndAttendanceDate(lr.getEmployee().getId(), lr.getLeaveDate())
                .ifPresentOrElse(
                        a -> {
                            a.setStatus(AttendanceRecord.AttendanceStatus.PAID_LEAVE);
                            a.setRemarks("Approved paid leave");
                            attendanceRepository.save(a);
                        },
                        () -> attendanceRepository.save(AttendanceRecord.builder()
                                .employee(lr.getEmployee()).attendanceDate(lr.getLeaveDate())
                                .status(AttendanceRecord.AttendanceStatus.PAID_LEAVE)
                                .remarks("Approved paid leave").workedMinutes(0).build()));
        log.info("LeaveServiceImpl:approve :: SUCCESS id={}", id);
        return toResponse(leaveRequestRepository.save(lr));
    }

    @Override
    @Transactional
    public LeaveResponse reject(Long id, String rejectionReason, String rejectedBy) {
        log.info("LeaveServiceImpl:reject :: id={} by={}", id, rejectedBy);
        LeaveRequest lr = getRequest(id);
        if (lr.getStatus() != LeaveRequest.LeaveStatus.PENDING)
            throw new BusinessException("Only PENDING leave requests can be rejected");
        lr.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        lr.setRejectionReason(rejectionReason);
        lr.setApprovedBy(rejectedBy);
        lr.setApprovedDate(LocalDateTime.now());
        return toResponse(leaveRequestRepository.save(lr));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveResponse> search(Long empId, LeaveRequest.LeaveStatus status, Pageable pageable) {
        return leaveRequestRepository.search(empId, status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves(Long empId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedDateDesc(empId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveBalanceResponse getBalance(Long empId, int year, int month) {
        Employee emp = employeeService.getEmployee(empId);
        LeaveBalance bal = getOrCreateBalance(empId, year, month);
        return LeaveBalanceResponse.builder().employeeId(empId).employeeName(emp.getName())
                .year(year).month(month).allocated(bal.getAllocated()).used(bal.getUsed()).balance(bal.getBalance()).build();
    }

    @Override
    @Transactional
    public void allocateMonthlyLeaves(int year, int month) {
        int allocation = settingsService.getIntValue(SettingKey.PAID_LEAVES_PER_MONTH);
        List<Employee> activeEmps = employeeService.getActiveEmployees();
        for (Employee emp : activeEmps) {
            if (leaveBalanceRepository.findByEmployeeIdAndYearAndMonth(emp.getId(), year, month).isEmpty()) {
                leaveBalanceRepository.save(LeaveBalance.builder().employee(emp).year(year).month(month)
                        .allocated(allocation).used(0).balance(allocation).build());
            }
        }
        log.info("LeaveServiceImpl:allocateMonthlyLeaves :: Allocated {} leaves to {} employees for {}/{}", allocation, activeEmps.size(), year, month);
    }

    private LeaveBalance getOrCreateBalance(Long empId, int year, int month) {
        return leaveBalanceRepository.findByEmployeeIdAndYearAndMonth(empId, year, month).orElseGet(() -> {
            int allocation = settingsService.getIntValue(SettingKey.PAID_LEAVES_PER_MONTH);
            Employee emp = employeeService.getEmployee(empId);
            return leaveBalanceRepository.save(LeaveBalance.builder().employee(emp).year(year).month(month)
                    .allocated(allocation).used(0).balance(allocation).build());
        });
    }

    private LeaveRequest getRequest(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));
    }

    private LeaveResponse toResponse(LeaveRequest lr) {
        return LeaveResponse.builder().id(lr.getId()).employeeId(lr.getEmployee().getId())
                .employeeCode(lr.getEmployee().getEmployeeCode()).employeeName(lr.getEmployee().getName())
                .leaveDate(lr.getLeaveDate()).reason(lr.getReason()).status(lr.getStatus().name())
                .approvedBy(lr.getApprovedBy()).approvedDate(lr.getApprovedDate())
                .rejectionReason(lr.getRejectionReason()).createdDate(lr.getCreatedDate()).build();
    }
}
