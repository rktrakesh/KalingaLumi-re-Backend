package com.business.erp.settings.repository;

import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.enums.SettingCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findBySettingKey(String settingKey);

    List<AppSetting> findBySettingCategory(SettingCategory settingCategory);
}
