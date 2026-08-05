package com.business.erp.payroll.engine;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.holiday.entity.Holiday;
import com.business.erp.holiday.repository.HolidayRepository;
import com.business.erp.leave.repository.LeaveRequestRepository;
import com.business.erp.overtime.repository.OvertimeRepository;
import com.business.erp.payroll.dto.response.PayrollExceptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scans a payroll period for the anomalies called out in the spec (missing check-out,
 * missing attendance, excessive overtime, pending OT approval, leave/holiday conflicts,
 * invalid attendance) so they can be resolved before payroll is approved.
 */
@Component
@RequiredArgsConstructor
public class PayrollExceptionReportService {

    /** Above this many approved OT minutes in a month, flag EXCESSIVE_OVERTIME (60 hours). */
    private static final int EXCESSIVE_OT_MINUTES_THRESHOLD = 60 * 60;

    private final EmployeeService employeeService;
    private final AttendanceRepository attendanceRepository;
    private final OvertimeRepository overtimeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<PayrollExceptionResponse> generate(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        Map<LocalDate, Holiday> holidaysByDate = new HashMap<>();
        holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(from, to)
                .forEach(h -> holidaysByDate.put(h.getHolidayDate(), h));

        List<PayrollExceptionResponse> results = new ArrayList<>();

        for (Employee emp : employeeService.getAttendanceEligibleEmployees()) {
            Map<String, List<LocalDate>> issues = new LinkedHashMap<>();

            List<AttendanceRecord> records = attendanceRepository
                    .findByEmployeeIdAndAttendanceDateBetween(emp.getId(), from, to);
            Map<LocalDate, AttendanceRecord> recordsByDate = records.stream()
                    .collect(Collectors.toMap(AttendanceRecord::getAttendanceDate, r -> r, (a, b) -> a));

            for (AttendanceRecord r : records) {
                if (r.getStatus() == AttendanceRecord.AttendanceStatus.PENDING_CHECKOUT) {
                    add(issues, "MISSING_CHECKOUT", r.getAttendanceDate());
                }
                if (r.getCheckIn() != null && r.getCheckOut() != null && r.getCheckOut().isBefore(r.getCheckIn())) {
                    add(issues, "INVALID_ATTENDANCE", r.getAttendanceDate());
                }
            }

            List<LocalDate> approvedLeaveDates = leaveRequestRepository.findApprovedLeaveDates(emp.getId(), from, to);
            for (LocalDate leaveDate : approvedLeaveDates) {
                AttendanceRecord r = recordsByDate.get(leaveDate);
                if (r != null && r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT) {
                    add(issues, "LEAVE_CONFLICT", leaveDate);
                }
            }

            for (Map.Entry<LocalDate, Holiday> e : holidaysByDate.entrySet()) {
                Holiday h = e.getValue();
                boolean blocked = h.getHolidayType() == Holiday.HolidayType.FACTORY_HOLIDAY
                        || !Boolean.TRUE.equals(h.getWorkAllowed());
                AttendanceRecord r = recordsByDate.get(e.getKey());
                if (blocked && r != null && r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT) {
                    add(issues, "HOLIDAY_CONFLICT", e.getKey());
                }
            }

            if (!overtimeRepository.findPendingInRange(emp.getId(), from, to).isEmpty()) {
                overtimeRepository.findPendingInRange(emp.getId(), from, to)
                        .forEach(o -> add(issues, "PENDING_OT_APPROVAL", o.getOvertimeDate()));
            }

            Integer approvedOtMinutes = overtimeRepository.sumApprovedMinutesByEmployeeAndMonth(emp.getId(), from, to);
            if (approvedOtMinutes != null && approvedOtMinutes > EXCESSIVE_OT_MINUTES_THRESHOLD) {
                add(issues, "EXCESSIVE_OVERTIME", to);
            }

            // MISSING_ATTENDANCE: a working day (not weekend/holiday/leave) with no record at all.
            for (LocalDate d = from; !d.isAfter(to) && !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
                if (holidaysByDate.containsKey(d)) continue;
                if (approvedLeaveDates.contains(d)) continue;
                if (!recordsByDate.containsKey(d)) {
                    add(issues, "MISSING_ATTENDANCE", d);
                }
            }

            if (!issues.isEmpty()) {
                List<LocalDate> allDates = issues.values().stream().flatMap(List::stream)
                        .distinct().sorted().collect(Collectors.toList());
                results.add(PayrollExceptionResponse.builder()
                        .employeeId(emp.getId()).employeeCode(emp.getEmployeeCode()).employeeName(emp.getName())
                        .issues(new ArrayList<>(issues.keySet())).affectedDates(allDates).build());
            }
        }
        return results;
    }

    private void add(Map<String, List<LocalDate>> issues, String type, LocalDate date) {
        issues.computeIfAbsent(type, k -> new ArrayList<>()).add(date);
    }
}