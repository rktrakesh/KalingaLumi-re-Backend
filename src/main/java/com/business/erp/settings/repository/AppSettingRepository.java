package com.business.erp.settings.repository;

import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.enums.SettingCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findBySettingKey(String settingKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select setting from AppSetting setting where setting.settingKey = :settingKey")
    Optional<AppSetting> lockBySettingKey(@Param("settingKey") String settingKey);

    List<AppSetting> findBySettingCategory(SettingCategory settingCategory);
}
