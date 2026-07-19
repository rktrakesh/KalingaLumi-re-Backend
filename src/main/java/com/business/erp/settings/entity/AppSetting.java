package com.business.erp.settings.entity;

import com.business.erp.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;
    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;
    @Column(length = 500)
    private String description;
    @Column(name = "effective_from_date", nullable = false)
    private LocalDate effectiveFromDate;
}
