package com.business.erp.settings.repository;
import com.business.erp.settings.entity.AppSettingSchedule;
import com.business.erp.settings.enums.SettingScheduleStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
public interface AppSettingScheduleRepository extends JpaRepository<AppSettingSchedule,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select s from AppSettingSchedule s where s.settingKey=:key and s.effectiveDate=:date and s.status=:status")
 List<AppSettingSchedule> lockPendingBySettingKeyAndEffectiveDate(@Param("key") String key, @Param("date") LocalDate date, @Param("status") SettingScheduleStatus status);
 List<AppSettingSchedule> findByStatusAndEffectiveDateLessThanEqual(SettingScheduleStatus status, LocalDate date);
 Optional<AppSettingSchedule> findFirstBySettingKeyAndStatusOrderByEffectiveDateAsc(String key, SettingScheduleStatus status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from AppSettingSchedule s where s.id=:id") Optional<AppSettingSchedule> lockById(@Param("id") Long id);
}
