package com.business.erp.common.exception;

import java.time.LocalDate;

public class AttendanceLockedException extends RuntimeException {
    public AttendanceLockedException(LocalDate date) {
        super("Attendance for " + date + " is locked by payroll and cannot be edited. " +
                "Reopen the corresponding payroll run first.");
    }
}