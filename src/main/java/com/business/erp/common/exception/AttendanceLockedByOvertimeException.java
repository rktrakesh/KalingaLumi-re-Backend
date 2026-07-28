package com.business.erp.common.exception;

public class AttendanceLockedByOvertimeException extends RuntimeException {
    public AttendanceLockedByOvertimeException(Long overtimeRequestId) {
        super("Attendance is locked because an approved overtime request exists. " +
                "Reopen overtime request #" + overtimeRequestId + " before correcting this attendance record.");
    }
}