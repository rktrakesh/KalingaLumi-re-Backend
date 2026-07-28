package com.business.erp.overtime.entity;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "overtime_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private AttendanceRecord attendance;

    @Column(name = "overtime_date", nullable = false)
    private LocalDate overtimeDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "request_type", nullable = false)
    private OvertimeType requestType;

    @Column(name = "requested_minutes", nullable = false)
    private Integer requestedMinutes;

    @Column(name = "approved_minutes")
    private Integer approvedMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    @Builder.Default
    private OvertimeStatus status = OvertimeStatus.PENDING;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "reopened_by", length = 50)
    private String reopenedBy;
    @Column(name = "reopened_date")
    private LocalDateTime reopenedDate;
    @Column(name = "reopen_reason", columnDefinition = "TEXT")
    private String reopenReason;

    public enum OvertimeType {EXCESS_HOURS, LEAVE_CONVERSION}

    public enum OvertimeStatus {PENDING, APPROVED, REJECTED, MODIFIED}
}