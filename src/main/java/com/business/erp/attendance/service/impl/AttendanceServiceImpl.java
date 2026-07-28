package com.business.erp.attendance.service.impl;

import com.business.erp.attendance.dto.AttendanceResponse;
import com.business.erp.attendance.dto.CheckInRequest;
import com.business.erp.attendance.dto.CheckOutRequest;
import com.business.erp.attendance.dto.CorrectAttendanceRequest;
import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.attendance.service.AttendanceService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.AttendanceLockedByOvertimeException;
import com.business.erp.common.exception.AttendanceLockedException;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.MonthClosedException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.monthclosing.service.MonthClosingService;
import com.business.erp.notification.service.NotificationService;
import com.business.erp.overtime.dto.response.OvertimeResponse;
import com.business.erp.overtime.service.OvertimeService;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;
    private final SettingsService settingsService;
    private final MonthClosingService monthClosingService;
    private final NotificationService notificationService;
    private final OvertimeService overtimeService;
    private final AuditService auditService;
    private final Logger log = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    @Override
    @Transactional
    public AttendanceResponse checkIn(CheckInRequest req, String createdBy) {
        log.info("AttendanceServiceImpl:checkIn :: empId={} date={}", req.getEmployeeId(), req.getAttendanceDate());
        Employee emp = employeeService.getEmployee(req.getEmployeeId());
        if (emp.getStatus() != Employee.EmployeeStatus.ACTIVE)
            throw new BusinessException("Employee is not active: " + emp.getEmployeeCode());
        if (attendanceRepository.findByEmployeeIdAndAttendanceDate(req.getEmployeeId(), req.getAttendanceDate()).isPresent())
            throw new BusinessException("Attendance already marked for " + emp.getName() + " on " + req.getAttendanceDate());
        AttendanceRecord record = attendanceRepository.save(AttendanceRecord.builder()
                .employee(emp).attendanceDate(req.getAttendanceDate()).checkIn(req.getCheckIn())
                .status(AttendanceRecord.AttendanceStatus.PRESENT).remarks(req.getRemarks()).workedMinutes(0).build());
        log.info("AttendanceServiceImpl:checkIn :: SUCCESS id={} emp={}", record.getId(), emp.getEmployeeCode());
        return toResponse(record);
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(Long attendanceId, CheckOutRequest req) {
        log.info("AttendanceServiceImpl:checkOut :: attendanceId={}", attendanceId);
        AttendanceRecord record = getRecord(attendanceId);
        if (Boolean.TRUE.equals(record.getLockedForPayroll()))
            throw new AttendanceLockedException(record.getAttendanceDate());
        if (record.getCheckIn() == null) throw new BusinessException("Cannot check out without check-in");
        if (record.getCheckOut() != null) throw new BusinessException("Already checked out");
        record.setCheckOut(req.getCheckOut());
        int worked = (int) ChronoUnit.MINUTES.between(record.getCheckIn(), req.getCheckOut());
        if (worked < 0) worked += 1440;
        record.setWorkedMinutes(worked);
        record.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
        if (req.getRemarks() != null) record.setRemarks(req.getRemarks());
        AttendanceRecord saved = attendanceRepository.save(record);
        int stdMins = settingsService.getIntValue(SettingKey.STANDARD_WORKING_HOURS) * 60;
        if (worked > stdMins) {
            log.info("AttendanceServiceImpl:checkOut :: Overtime detected empId={} extra={}min",
                    record.getEmployee().getId(), worked - stdMins);
            overtimeService.createOvertimeRequest(saved, worked - stdMins);
        }
        log.info("AttendanceServiceImpl:checkOut :: SUCCESS id={} workedMin={}", saved.getId(), worked);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttendanceResponse correct(Long id, CorrectAttendanceRequest req, String updatedBy) {
        log.info("AttendanceServiceImpl:correct :: id={} by={}", id, updatedBy);
        AttendanceRecord record = getRecord(id);
        if (Boolean.TRUE.equals(record.getLockedForPayroll()))
            throw new AttendanceLockedException(record.getAttendanceDate());
        if (monthClosingService.isMonthClosed(record.getAttendanceDate().getYear(),
                record.getAttendanceDate().getMonthValue()))
            throw new MonthClosedException(record.getAttendanceDate().getYear(),
                    record.getAttendanceDate().getMonthValue());
        if (record.getAttendanceDate().isBefore(LocalDate.now().minusDays(7)))
            throw new BusinessException("Attendance older than 7 days cannot be edited");

        // Attendance Edited -> Was OT Approved? -> YES -> Block Editing. An admin must explicitly
        // reopen the approved/modified overtime request first (OvertimeService.reopen) before this
        // attendance record becomes editable again — editing underneath an approved OT figure would
        // otherwise silently invalidate an approval nobody reviewed the correction against.
        java.util.Optional<OvertimeResponse> activeOt = overtimeService.findActiveExcessHoursRequest(id);
        boolean otIsApprovedLike = activeOt.isPresent()
                && ("APPROVED".equals(activeOt.get().getStatus()) || "MODIFIED".equals(activeOt.get().getStatus()));
        if (otIsApprovedLike) {
            throw new AttendanceLockedByOvertimeException(activeOt.get().getId());
        }

        Object oldVal = toResponse(record);
        if (req.getCheckIn() != null) record.setCheckIn(req.getCheckIn());
        if (req.getCheckOut() != null) {
            record.setCheckOut(req.getCheckOut());
            if (record.getCheckIn() != null) {
                int worked = (int) ChronoUnit.MINUTES.between(record.getCheckIn(), req.getCheckOut());
                if (worked < 0) worked += 1440;
                record.setWorkedMinutes(worked);
            }
        }
        if (req.getStatus() != null) {
            record.setStatus(req.getStatus());
        } else if (req.getCheckOut() != null && record.getCheckIn() != null
                && record.getStatus() == AttendanceRecord.AttendanceStatus.PENDING_CHECKOUT) {
            record.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
        }
        record.setRemarks(req.getRemarks());
        AttendanceRecord saved = attendanceRepository.save(record);
        auditService.log("ATTENDANCE", "CORRECT", "AttendanceRecord", id, oldVal, toResponse(saved));

        if (saved.getWorkedMinutes() != null && saved.getWorkedMinutes() > 0 && activeOt.isEmpty()) {
            int stdMins = settingsService.getIntValue(SettingKey.STANDARD_WORKING_HOURS) * 60;
            if (saved.getWorkedMinutes() > stdMins) {
                log.info("AttendanceServiceImpl:correct :: Overtime recalculated on correction empId={} date={} extra={}min",
                        saved.getEmployee().getId(), saved.getAttendanceDate(), saved.getWorkedMinutes() - stdMins);
                overtimeService.createOvertimeRequest(saved, saved.getWorkedMinutes() - stdMins);
            }
        }

        log.info("AttendanceServiceImpl:correct :: SUCCESS id={}", id);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> search(Long employeeId, LocalDate date,
                                                   AttendanceRecord.AttendanceStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        log.debug("AttendanceServiceImpl:search :: empId={} status={} from={} to={}", employeeId, status, from, to);
        return PageResponse.of(attendanceRepository.search(employeeId, date, status, from, to, pageable)
                .map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMonthlyAttendance(Long employeeId, int year, int month) {
        log.debug("AttendanceServiceImpl:getMonthlyAttendance :: empId={} {}/{}", employeeId, year, month);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employeeId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getPendingCheckouts() {
        return attendanceRepository.findByStatus(AttendanceRecord.AttendanceStatus.PENDING_CHECKOUT)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markPendingCheckouts() {
        LocalDate today = LocalDate.now();
        List<AttendanceRecord> missed = attendanceRepository
                .findByAttendanceDateAndStatus(today, AttendanceRecord.AttendanceStatus.PRESENT)
                .stream().filter(r -> r.getCheckOut() == null).collect(Collectors.toList());
        for (AttendanceRecord r : missed) {
            r.setStatus(AttendanceRecord.AttendanceStatus.PENDING_CHECKOUT);
            attendanceRepository.save(r);
            notificationService.createForgottenCheckoutNotification(r.getEmployee(), r.getAttendanceDate());
        }
        log.info("AttendanceServiceImpl:markPendingCheckouts :: Marked {} records as PENDING_CHECKOUT", missed.size());
    }

    @Override
    @Transactional
    public void lockForPayroll(LocalDate from, LocalDate to, Long payrollRunId) {
        int locked = attendanceRepository.lockRange(from, to, payrollRunId);
        log.info("AttendanceServiceImpl:lockForPayroll :: from={} to={} runId={} recordsLocked={}",
                from, to, payrollRunId, locked);
    }

    @Override
    @Transactional
    public void unlockForPayroll(Long payrollRunId) {
        int unlocked = attendanceRepository.unlockByRunId(payrollRunId);
        log.info("AttendanceServiceImpl:unlockForPayroll :: runId={} recordsUnlocked={}", payrollRunId, unlocked);
    }

    private AttendanceRecord getRecord(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceRecord", id));
    }

    public AttendanceResponse toResponse(AttendanceRecord r) {
        return AttendanceResponse.builder()
                .id(r.getId()).employeeId(r.getEmployee().getId())
                .employeeCode(r.getEmployee().getEmployeeCode()).employeeName(r.getEmployee().getName())
                .attendanceDate(r.getAttendanceDate()).checkIn(r.getCheckIn()).checkOut(r.getCheckOut())
                .workedMinutes(r.getWorkedMinutes()).status(r.getStatus().name()).remarks(r.getRemarks())
                .createdBy(r.getCreatedBy()).createdDate(r.getCreatedDate())
                .updatedBy(r.getUpdatedBy()).updatedDate(r.getUpdatedDate())
                .lockedForPayroll(r.getLockedForPayroll()).lockedByPayrollRunId(r.getLockedByPayrollRunId()).build();
    }
}