package com.business.erp.attendance.service;

import com.business.erp.attendance.dto.AttendanceResponse;
import com.business.erp.attendance.dto.CheckInRequest;
import com.business.erp.attendance.dto.CheckOutRequest;
import com.business.erp.attendance.dto.CorrectAttendanceRequest;
import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    AttendanceResponse checkIn(CheckInRequest request, String createdBy);

    AttendanceResponse checkOut(Long attendanceId, CheckOutRequest request);

    AttendanceResponse getById(Long attendanceId);

    AttendanceResponse correct(Long id, CorrectAttendanceRequest request, String updatedBy);

    PageResponse<AttendanceResponse> search(Long employeeId, LocalDate date,
                                            AttendanceRecord.AttendanceStatus status, LocalDate from, LocalDate to, Pageable pageable);

    List<AttendanceResponse> getMonthlyAttendance(Long employeeId, int year, int month);

    List<AttendanceResponse> getPendingCheckouts();

    void markPendingCheckouts();

    /** Locks every attendance record in [from, to] against edits, tagging it with the payroll run that locked it. */
    void lockForPayroll(LocalDate from, LocalDate to, Long payrollRunId);

    /** Releases the payroll lock previously applied by the given run (used when a payroll run is reopened). */
    void unlockForPayroll(Long payrollRunId);
}
