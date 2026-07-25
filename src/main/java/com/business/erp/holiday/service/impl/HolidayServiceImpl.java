package com.business.erp.holiday.service.impl;

import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.repository.AttendanceRepository;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.holiday.dto.request.CreateHolidayRequest;
import com.business.erp.holiday.dto.response.HolidayResponse;
import com.business.erp.holiday.entity.Holiday;
import com.business.erp.holiday.repository.HolidayRepository;
import com.business.erp.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;
    private final Logger log = LoggerFactory.getLogger(HolidayServiceImpl.class);

    @Override
    @Transactional
    public HolidayResponse create(CreateHolidayRequest req) {
        log.info("HolidayServiceImpl:create :: date={} name={}", req.getHolidayDate(), req.getName());
        if (holidayRepository.existsByHolidayDate(req.getHolidayDate()))
            throw new BusinessException("Holiday already exists for date: " + req.getHolidayDate());
        Holiday holiday = holidayRepository.save(Holiday.builder()
                .holidayDate(req.getHolidayDate()).name(req.getName()).holidayType(req.getHolidayType())
                .workAllowed(req.isWorkAllowed()).applicableState(req.getApplicableState()).build());
        List<Employee> activeEmps = employeeService.getActiveEmployees();
        int created = 0;
        for (Employee emp : activeEmps) {
            if (attendanceRepository.findByEmployeeIdAndAttendanceDate(emp.getId(), req.getHolidayDate()).isEmpty()) {
                attendanceRepository.save(AttendanceRecord.builder()
                        .employee(emp).attendanceDate(req.getHolidayDate())
                        .status(AttendanceRecord.AttendanceStatus.HOLIDAY)
                        .remarks("Holiday: " + req.getName()).workedMinutes(0).build());
                created++;
            }
        }
        log.info("HolidayServiceImpl:create :: SUCCESS holidayId={} attendanceCreated={}", holiday.getId(), created);
        return toResponse(holiday);
    }

    @Override
    public List<HolidayResponse> getByYear(int year) {
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("HolidayServiceImpl:delete :: id={}", id);
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", id));
        List<AttendanceRecord> records = attendanceRepository
                .findByAttendanceDateAndStatus(holiday.getHolidayDate(), AttendanceRecord.AttendanceStatus.HOLIDAY);
        records.forEach(r -> {
            r.setStatus(AttendanceRecord.AttendanceStatus.ABSENT);
            r.setRemarks("Holiday removed: " + holiday.getName());
        });
        attendanceRepository.saveAll(records);
        holidayRepository.delete(holiday);
        log.info("HolidayServiceImpl:delete :: SUCCESS id={} attendanceReverted={}", id, records.size());
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsByHolidayDate(date);
    }

    private HolidayResponse toResponse(Holiday h) {
        return HolidayResponse.builder().id(h.getId()).holidayDate(h.getHolidayDate())
                .name(h.getName()).holidayType(h.getHolidayType().name())
                .workAllowed(h.getWorkAllowed()).applicableState(h.getApplicableState()).build();
    }
}