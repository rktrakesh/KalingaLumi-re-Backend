package com.business.erp.overtime.service;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.common.response.PageResponse;
import com.business.erp.overtime.dto.request.ApproveOvertimeRequest;
import com.business.erp.overtime.dto.request.ConvertLeaveToOTRequest;
import com.business.erp.overtime.dto.request.ReopenOvertimeRequest;
import com.business.erp.overtime.dto.response.OvertimeResponse;
import com.business.erp.overtime.entity.OvertimeRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OvertimeService {
    void createOvertimeRequest(AttendanceRecord attendance, int overtimeMinutes);

    java.util.Optional<OvertimeResponse> findActiveExcessHoursRequest(Long attendanceId);

    OvertimeResponse reopen(Long id, ReopenOvertimeRequest request, String reopenedBy);

    OvertimeResponse approve(Long id, ApproveOvertimeRequest request, String approvedBy);

    OvertimeResponse reject(Long id, String remarks, String rejectedBy);

    List<OvertimeResponse> convertUnusedLeaves(ConvertLeaveToOTRequest request, String createdBy);

    PageResponse<OvertimeResponse> search(Long empId, OvertimeRequest.OvertimeStatus status, Pageable pageable);

    Integer getApprovedMinutesForPayroll(Long empId, LocalDate from, LocalDate to);

    /** Approved overtime minutes per calendar date within the range — used to bucket OT by day classification. */
    java.util.Map<LocalDate, Integer> getApprovedMinutesByDate(Long empId, LocalDate from, LocalDate to);
}