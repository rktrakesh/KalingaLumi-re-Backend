package com.business.erp.settings.repository;

import com.business.erp.settings.entity.AppSettingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.business.erp.settings.enums.SettingHistoryEventType;

public interface AppSettingHistoryRepository extends JpaRepository<AppSettingHistory, Long> {
    List<AppSettingHistory> findBySettingKeyOrderByChangedDateDesc(String settingKey);

    @Query("SELECT h FROM AppSettingHistory h WHERE h.settingKey = :key " +
            "AND h.effectiveFromDate <= :asOfDate ORDER BY h.effectiveFromDate DESC, h.changedDate DESC, h.id DESC")
    List<AppSettingHistory> findAsOf(@Param("key") String key, @Param("asOfDate") LocalDate asOfDate);

    default Optional<AppSettingHistory> findMostRecentAsOf(String key, LocalDate asOfDate) {
        List<AppSettingHistory> results = findAsOf(key, asOfDate).stream()
                .filter(h -> h.getChangeType() == SettingHistoryEventType.BASELINE || h.getChangeType() == SettingHistoryEventType.IMMEDIATE_UPDATE || h.getChangeType() == SettingHistoryEventType.ACTIVATED).toList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Query("SELECT MAX(h.effectiveFromDate) FROM AppSettingHistory h WHERE h.settingKey = :key AND h.changeType = 'BASELINE'")
    Optional<LocalDate> findCutoverDate(@Param("key") String key);
}
