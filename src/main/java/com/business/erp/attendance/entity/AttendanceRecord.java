package com.business.erp.attendance.entity;

import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(name = "uq_attendance_emp_date",
                columnNames = {"employee_id", "attendance_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in")
    private LocalTime checkIn;

    @Column(name = "check_out")
    private LocalTime checkOut;

    @Column(name = "worked_minutes", nullable = false)
    @Builder.Default
    private Integer workedMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private AttendanceStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    /**
     * True once a payroll run covering this date has reached APPROVED status or beyond.
     * While locked, this record is read-only to everyone except the payroll reopen flow.
     * Attendance facts themselves are never altered by payroll — this is purely a freeze flag.
     */
    @Column(name = "locked_for_payroll", nullable = false)
    @Builder.Default
    private Boolean lockedForPayroll = false;

    /** The payroll run that locked this record, for traceability. Null while unlocked. */
    @Column(name = "locked_by_payroll_run_id")
    private Long lockedByPayrollRunId;

    public enum AttendanceStatus {PRESENT, ABSENT, PAID_LEAVE, HOLIDAY, PENDING_CHECKOUT}
}