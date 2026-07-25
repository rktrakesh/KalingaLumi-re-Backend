package com.business.erp.payroll.engine;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.holiday.entity.Holiday;
import com.business.erp.holiday.repository.HolidayRepository;
import com.business.erp.leave.repository.LeaveRequestRepository;
import com.business.erp.overtime.service.OvertimeService;
import com.business.erp.payroll.engine.calendar.DayClassificationInput;
import com.business.erp.payroll.engine.calendar.PayrollCalendar;
import com.business.erp.payroll.engine.calendar.PayrollDay;
import com.business.erp.payroll.engine.calendar.PayrollDayClassifier;
import com.business.erp.payroll.engine.leave.LeaveResolutionContext;
import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Responsible ONLY for determining the status of every day in a payroll period for one
 * employee — it never calculates money. Gathers the raw facts (attendance, holidays,
 * approved leave, approved OT-by-date) and hands each day to {@link PayrollDayClassifier}
 * one at a time, in chronological order (required so automatic-leave allocation
 * consumes balance fairly). The result is a {@link PayrollCalendar} — the only thing
 * {@code PayrollCalculationEngine} is allowed to consume.
 */
@Component
@RequiredArgsConstructor
public class PayrollCalendarEngine {

    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final OvertimeService overtimeService;
    private final PayrollDayClassifier classifier;
    private final PayrollPolicyService policyService;
    private final Logger log = LoggerFactory.getLogger(PayrollCalendarEngine.class);

    /** Loads holidays for a period once, for the caller to share across every employee in the run. */
    public Map<LocalDate, Holiday> loadHolidays(LocalDate from, LocalDate to) {
        Map<LocalDate, Holiday> holidaysByDate = new HashMap<>();
        holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(from, to)
                .forEach(h -> holidaysByDate.put(h.getHolidayDate(), h));
        return holidaysByDate;
    }

    public PayrollCalendar buildCalendar(Long employeeId, LocalDate from, LocalDate to, PayrollSettingsSnapshot snapshot) {
        return buildCalendar(employeeId, from, to, snapshot, loadHolidays(from, to));
    }

    /**
     * Same as {@link #buildCalendar(Long, LocalDate, LocalDate, PayrollSettingsSnapshot)} but takes
     * an already-fetched holiday map. Holidays are identical for every employee in a run, so
     * {@code PayrollServiceImpl} loads them once per period and passes them into every
     * per-employee call — one query instead of N (repository optimization, item 5).
     */
    public PayrollCalendar buildCalendar(Long employeeId, LocalDate from, LocalDate to,
                                         PayrollSettingsSnapshot snapshot, Map<LocalDate, Holiday> holidaysByDate) {
        Set<DayOfWeek> weeklyOffDays = policyService.parseWeeklyOffDays(snapshot);

        Set<LocalDate> approvedLeaveDates = new HashSet<>(
                leaveRequestRepository.findApprovedLeaveDates(employeeId, from, to));

        Map<LocalDate, AttendanceRecord> attendanceByDate = new HashMap<>();
        attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, from, to)
                .forEach(a -> attendanceByDate.put(a.getAttendanceDate(), a));

        Map<LocalDate, Integer> approvedOtByDate = overtimeService.getApprovedMinutesByDate(employeeId, from, to);

        int year = from.getYear(), month = from.getMonthValue();
        List<PayrollDay> days = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            AttendanceRecord attendance = attendanceByDate.get(date);
            int workedMinutes = attendance != null && attendance.getWorkedMinutes() != null ? attendance.getWorkedMinutes() : 0;
            boolean attendancePresent = attendance != null && attendance.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT;

            DayClassificationInput input = DayClassificationInput.builder()
                    .date(date)
                    .holiday(holidaysByDate.get(date))
                    .weeklyOffDayOfWeek(weeklyOffDays.contains(date.getDayOfWeek()))
                    .workedMinutes(workedMinutes)
                    .attendanceExists(attendance != null)
                    .attendancePresent(attendancePresent)
                    .build();

            LeaveResolutionContext leaveContext = LeaveResolutionContext.builder()
                    .employeeId(employeeId).year(year).month(month)
                    .approvedLeaveDates(approvedLeaveDates)
                    .workedToday(attendancePresent && workedMinutes > 0)
                    .build();

            int approvedOtForDate = approvedOtByDate.getOrDefault(date, 0);
            days.add(classifier.classify(input, leaveContext, approvedOtForDate));
        }

        PayrollCalendar calendar = new PayrollCalendar(employeeId, from, to, days);
        log.debug("PayrollCalendarEngine:buildCalendar :: empId={} {} to {} present={} weeklyOff={}(worked={}) " +
                        "holiday={}(worked={}) approvedLeave={} autoLeave={} absent={}",
                employeeId, from, to, calendar.presentDays(), calendar.weeklyOffDays(), calendar.weeklyOffWorkedDays(),
                calendar.holidayDays(), calendar.holidayWorkedDays(), calendar.approvedPaidLeaveDays(),
                calendar.automaticPaidLeaveDays(), calendar.absentDays());
        return calendar;
    }
}