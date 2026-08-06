package com.business.erp.settings.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.business.erp.settings.enums.SettingHistoryEventType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_settings_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "change_type", nullable = false, length = 30)
    private SettingHistoryEventType changeType;
    @Column(name = "old_value", length = 500)
    private String oldValue;
    @Column(name = "new_value", nullable = false, length = 500)
    private String newValue;
    @Column(name = "effective_from_date", nullable = false)
    private LocalDate effectiveFromDate;
    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;
    @Column(name = "changed_date", nullable = false)
    private LocalDateTime changedDate;
}
