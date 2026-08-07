package com.business.erp.attendance.repository;

import com.business.erp.attendance.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    List<AttendanceRecord> findByAttendanceDateAndStatus(LocalDate date, AttendanceRecord.AttendanceStatus status);

    List<AttendanceRecord> findByStatus(AttendanceRecord.AttendanceStatus status);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.employee.id = :empId " +
            "AND a.attendanceDate BETWEEN :from AND :to " +
            "AND a.attendanceDate >= a.employee.joiningDate ORDER BY a.attendanceDate ASC")
    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            @Param("empId") Long empId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.attendanceDate >= a.employee.joiningDate AND " +
            "(:employeeId IS NULL OR a.employee.id = :employeeId) AND " +
            "(:date IS NULL OR a.attendanceDate = :date) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:from IS NULL OR a.attendanceDate >= :from) AND " +
            "(:to IS NULL OR a.attendanceDate <= :to) ORDER BY a.attendanceDate DESC")
    Page<AttendanceRecord> search(@Param("employeeId") Long employeeId, @Param("date") LocalDate date,
                                  @Param("status") AttendanceRecord.AttendanceStatus status,
                                  @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.attendanceDate = :date AND a.status = :status")
    long countByDateAndStatus(@Param("date") LocalDate date, @Param("status") AttendanceRecord.AttendanceStatus status);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.attendanceDate BETWEEN :from AND :to AND a.status = :status")
    long countByDateRangeAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                   @Param("status") AttendanceRecord.AttendanceStatus status);

    @Query("SELECT SUM(a.workedMinutes) FROM AttendanceRecord a WHERE a.employee.id = :empId " +
            "AND a.attendanceDate BETWEEN :from AND :to AND a.status = 'PRESENT'")
    Integer sumWorkedMinutesByEmployeeAndMonth(@Param("empId") Long empId,
                                               @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.employee.id = :empId " +
            "AND a.attendanceDate BETWEEN :from AND :to " +
            "AND a.attendanceDate >= a.employee.joiningDate")
    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetween(
            @Param("empId") Long empId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<AttendanceRecord> findByAttendanceDateBetween(LocalDate from, LocalDate to);

    // ── Payroll freeze support ──────────────────────────────────────────────
    @Modifying
    @Query("UPDATE AttendanceRecord a SET a.lockedForPayroll = true, a.lockedByPayrollRunId = :runId " +
            "WHERE a.attendanceDate BETWEEN :from AND :to")
    int lockRange(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("runId") Long runId);

    @Modifying
    @Query("UPDATE AttendanceRecord a SET a.lockedForPayroll = false, a.lockedByPayrollRunId = null " +
            "WHERE a.lockedByPayrollRunId = :runId")
    int unlockByRunId(@Param("runId") Long runId);

    boolean existsByAttendanceDateBetweenAndLockedForPayrollTrue(LocalDate from, LocalDate to);
}
