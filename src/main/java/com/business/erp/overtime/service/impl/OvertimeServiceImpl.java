package com.business.erp.overtime.service.impl;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.notification.service.NotificationService;
import com.business.erp.overtime.dto.request.ApproveOvertimeRequest;
import com.business.erp.overtime.dto.request.ConvertLeaveToOTRequest;
import com.business.erp.overtime.dto.response.OvertimeResponse;
import com.business.erp.overtime.entity.OvertimeRequest;
import com.business.erp.overtime.repository.OvertimeRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeServiceImpl implements OvertimeService {

    private final OvertimeRepository overtimeRepository;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final SettingsService settingsService;
    private final Logger log = LoggerFactory.getLogger(OvertimeServiceImpl.class);

    @Override
    @Transactional
    public void createOvertimeRequest(AttendanceRecord attendance, int overtimeMinutes) {
        log.info("OvertimeServiceImpl:createOvertimeRequest :: empId={} date={} minutes={}",
                attendance.getEmployee().getId(), attendance.getAttendanceDate(), overtimeMinutes);
        OvertimeRequest ot = overtimeRepository.save(OvertimeRequest.builder()
                .employee(attendance.getEmployee()).attendance(attendance)
                .overtimeDate(attendance.getAttendanceDate())
                .requestType(OvertimeRequest.OvertimeType.EXCESS_HOURS)
                .requestedMinutes(overtimeMinutes).status(OvertimeRequest.OvertimeStatus.PENDING).build());
        notificationService.createOvertimeApprovalNotification(attendance.getEmployee(), ot.getId());
        log.info("OvertimeServiceImpl:createOvertimeRequest :: SUCCESS otId={}", ot.getId());
    }

    @Override
    @Transactional
    public OvertimeResponse approve(Long id, ApproveOvertimeRequest req, String approvedBy) {
        log.info("OvertimeServiceImpl:approve :: id={} approvedMinutes={} by={}", id, req.getApprovedMinutes(), approvedBy);
        OvertimeRequest ot = getOT(id);
        if (ot.getStatus() != OvertimeRequest.OvertimeStatus.PENDING)
            throw new BusinessException("Only PENDING overtime requests can be approved");
        OvertimeRequest.OvertimeStatus status = req.getApprovedMinutes().equals(ot.getRequestedMinutes())
                ? OvertimeRequest.OvertimeStatus.APPROVED : OvertimeRequest.OvertimeStatus.MODIFIED;
        ot.setApprovedMinutes(req.getApprovedMinutes());
        ot.setStatus(status);
        ot.setApprovedBy(approvedBy);
        ot.setApprovedDate(LocalDateTime.now());
        if (req.getRemarks() != null) ot.setRemarks(req.getRemarks());
        log.info("OvertimeServiceImpl:approve :: SUCCESS id={} status={}", id, status);
        return toResponse(overtimeRepository.save(ot));
    }

    @Override
    @Transactional
    public OvertimeResponse reject(Long id, String remarks, String rejectedBy) {
        log.info("OvertimeServiceImpl:reject :: id={} by={}", id, rejectedBy);
        OvertimeRequest ot = getOT(id);
        if (ot.getStatus() != OvertimeRequest.OvertimeStatus.PENDING)
            throw new BusinessException("Only PENDING overtime requests can be rejected");
        ot.setStatus(OvertimeRequest.OvertimeStatus.REJECTED);
        ot.setApprovedBy(rejectedBy);
        ot.setApprovedDate(LocalDateTime.now());
        ot.setRemarks(remarks);
        log.info("OvertimeServiceImpl:reject :: SUCCESS id={}", id);
        return toResponse(overtimeRepository.save(ot));
    }

    @Override
    @Transactional
    public List<OvertimeResponse> convertUnusedLeaves(ConvertLeaveToOTRequest req, String createdBy) {
        log.info("OvertimeServiceImpl:convertUnusedLeaves :: empId={} days={} {}/{}", req.getEmployeeId(), req.getUnusedLeaveDays(), req.getYear(), req.getMonth());
        int stdHours = settingsService.getIntValue(SettingKey.STANDARD_WORKING_HOURS);
        List<OvertimeResponse> results = new ArrayList<>();
        for (int i = 0; i < req.getUnusedLeaveDays(); i++) {
            LocalDate date = LocalDate.of(req.getYear(), req.getMonth(), 1).plusDays(i);
            OvertimeRequest ot = overtimeRepository.save(OvertimeRequest.builder()
                    .employee(employeeService.getEmployee(req.getEmployeeId()))
                    .overtimeDate(date)
                    .requestType(OvertimeRequest.OvertimeType.LEAVE_CONVERSION)
                    .requestedMinutes(stdHours * 60)
                    .status(OvertimeRequest.OvertimeStatus.PENDING)
                    .remarks(req.getRemarks() != null ? req.getRemarks() : "Unused leave conversion").build());
            results.add(toResponse(ot));
        }
        log.info("OvertimeServiceImpl:convertUnusedLeaves :: SUCCESS created={}", results.size());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OvertimeResponse> search(Long empId, OvertimeRequest.OvertimeStatus status, Pageable pageable) {
        return PageResponse.of(overtimeRepository.search(empId, status, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getApprovedMinutesForPayroll(Long empId, LocalDate from, LocalDate to) {
        Integer mins = overtimeRepository.sumApprovedMinutesByEmployeeAndMonth(empId, from, to);
        return mins != null ? mins : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<LocalDate, Integer> getApprovedMinutesByDate(Long empId, LocalDate from, LocalDate to) {
        return overtimeRepository.findApprovedInRange(empId, from, to).stream()
                .collect(java.util.stream.Collectors.toMap(
                        OvertimeRequest::getOvertimeDate,
                        o -> o.getApprovedMinutes() != null ? o.getApprovedMinutes() : 0,
                        Integer::sum));
    }

    private OvertimeRequest getOT(Long id) {
        return overtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OvertimeRequest", id));
    }

    private OvertimeResponse toResponse(OvertimeRequest o) {
        return OvertimeResponse.builder()
                .id(o.getId()).employeeId(o.getEmployee().getId())
                .employeeCode(o.getEmployee().getEmployeeCode()).employeeName(o.getEmployee().getName())
                .overtimeDate(o.getOvertimeDate()).requestType(o.getRequestType().name())
                .requestedMinutes(o.getRequestedMinutes()).approvedMinutes(o.getApprovedMinutes())
                .status(o.getStatus().name()).approvedBy(o.getApprovedBy())
                .approvedDate(o.getApprovedDate()).remarks(o.getRemarks())
                .createdDate(o.getCreatedDate()).build();
    }
}