package com.business.erp.settings.entity;

import com.business.erp.settings.enums.SettingScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_setting_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettingSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "setting_key", nullable = false)
    private String settingKey;
    @Column(name = "scheduled_value", nullable = false)
    private String scheduledValue;
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private SettingScheduleStatus status;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
