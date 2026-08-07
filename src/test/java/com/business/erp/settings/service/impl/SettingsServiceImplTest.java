package com.business.erp.settings.service.impl;

import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.settings.dto.response.SettingUpdateResult;
import com.business.erp.settings.entity.*;
import com.business.erp.settings.enums.*;
import com.business.erp.settings.repository.*;
import com.business.erp.settings.service.SettingsActivationService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsServiceImplTest {
 @Mock AppSettingRepository settings; @Mock AppSettingHistoryRepository history; @Mock AppSettingScheduleRepository schedules;
 @Mock ClockProvider dates; @Mock SettingsActivationService activationService;
 SettingsServiceImpl service; final LocalDate today=LocalDate.of(2026,8,6);
 @BeforeEach void setUp(){ MockitoAnnotations.openMocks(this); when(dates.today()).thenReturn(today); service=new SettingsServiceImpl(settings,history,schedules,activationService,dates,Clock.fixed(today.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(),ZoneId.of("Asia/Kolkata"))); }
 private AppSetting setting(SettingKey key,String value){ return AppSetting.builder().id(1L).settingKey(key.name()).settingCategory(SettingCategory.ORGANIZATION).dataType("STRING").editable(true).settingValue(value).effectiveFromDate(today.minusDays(1)).description("test").build(); }
 @Test void immediateUpdate_changesActiveValue_withoutPendingSchedule(){ AppSetting s=setting(SettingKey.COMPANY_NAME,"Old"); when(settings.findBySettingKey(s.getSettingKey())).thenReturn(Optional.of(s)); when(settings.save(any())).thenAnswer(i->i.getArgument(0)); SettingUpdateResult r=service.updateSetting(SettingKey.COMPANY_NAME,"New","admin"); assertEquals("IMMEDIATE",r.getUpdateMode()); assertEquals("New",s.getSettingValue()); assertNull(r.getSetting().getPendingValue()); verify(history).save(argThat(h->h.getChangeType()==SettingHistoryEventType.IMMEDIATE_UPDATE)); verify(schedules,never()).save(any()); }
 @Test void scheduledUpdate_preservesActiveAndReturnsPending(){ AppSetting s=setting(SettingKey.STANDARD_WORKING_HOURS,"8"); AppSettingSchedule pending=AppSettingSchedule.builder().settingKey(s.getSettingKey()).scheduledValue("9").effectiveDate(today.plusMonths(1).withDayOfMonth(1)).status(SettingScheduleStatus.PENDING).createdBy("admin").createdDate(LocalDateTime.now()).build(); when(settings.findBySettingKey(s.getSettingKey())).thenReturn(Optional.of(s)); when(settings.save(any())).thenAnswer(i->i.getArgument(0)); when(schedules.findFirstBySettingKeyAndStatusOrderByEffectiveDateAsc(s.getSettingKey(),SettingScheduleStatus.PENDING)).thenReturn(Optional.of(pending)); SettingUpdateResult r=service.updateSetting(SettingKey.STANDARD_WORKING_HOURS,"9","admin"); assertEquals("SCHEDULED",r.getUpdateMode()); assertEquals("8",r.getSetting().getActiveValue()); assertEquals("9",r.getSetting().getPendingValue()); assertEquals(today.plusMonths(1).withDayOfMonth(1),r.getSetting().getPendingEffectiveDate()); assertEquals("PENDING",r.getSetting().getPendingStatus()); assertEquals(r.getSetting().getActiveValue(),r.getSetting().getSettingValue()); verify(schedules).save(any(AppSettingSchedule.class)); verify(history).save(argThat(h->h.getChangeType()==SettingHistoryEventType.SCHEDULED)); }
 @Test void replacement_marksOldScheduleAndLeavesOneSuccessor(){ AppSetting s=setting(SettingKey.STANDARD_WORKING_HOURS,"8"); AppSettingSchedule old=AppSettingSchedule.builder().id(4L).settingKey(s.getSettingKey()).scheduledValue("9").effectiveDate(today.plusMonths(1).withDayOfMonth(1)).status(SettingScheduleStatus.PENDING).createdBy("a").createdDate(LocalDateTime.now()).build(); when(settings.findBySettingKey(s.getSettingKey())).thenReturn(Optional.of(s)); when(settings.save(any())).thenAnswer(i->i.getArgument(0)); when(schedules.lockPendingBySettingKeyAndEffectiveDate(eq(s.getSettingKey()),any(),eq(SettingScheduleStatus.PENDING))).thenReturn(List.of(old)); SettingUpdateResult r=service.updateSetting(SettingKey.STANDARD_WORKING_HOURS,"10","admin"); assertTrue(r.isReplacedExistingPending()); assertEquals(SettingScheduleStatus.REPLACED,old.getStatus()); InOrder order=inOrder(schedules); order.verify(schedules).saveAllAndFlush(any()); order.verify(schedules).save(any(AppSettingSchedule.class)); verify(history,atLeastOnce()).save(argThat(h->h.getChangeType()==SettingHistoryEventType.REPLACED)); }
 @Test void historicalLookup_usesOnlyValueBearingHistoryAndRejectsMissingBaseline(){ when(history.findCutoverDate(SettingKey.COMPANY_NAME.name())).thenReturn(Optional.empty()); BusinessException ex=assertThrows(BusinessException.class,()->service.getValueAsOf(SettingKey.COMPANY_NAME,today)); assertTrue(ex.getMessage().contains("cutover")); }
 @Test void responseCompatibilityAlias_matchesActiveValue(){ AppSetting s=setting(SettingKey.COMPANY_NAME,"ERP System"); when(settings.findBySettingKey(s.getSettingKey())).thenReturn(Optional.of(s)); when(settings.save(any())).thenAnswer(i->i.getArgument(0)); SettingUpdateResult result=service.updateSetting(SettingKey.COMPANY_NAME,"ERP","admin"); assertEquals(result.getSetting().getActiveValue(),result.getSetting().getSettingValue()); assertEquals(today,result.getSetting().getActiveEffectiveFromDate()); assertNull(result.getSetting().getPendingValue()); }
}
