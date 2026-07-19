package com.business.erp.holiday.service;

import com.business.erp.holiday.dto.request.CreateHolidayRequest;
import com.business.erp.holiday.dto.response.HolidayResponse;

import java.time.LocalDate;
import java.util.List;

public interface HolidayService {
    HolidayResponse create(CreateHolidayRequest request);

    List<HolidayResponse> getByYear(int year);

    void delete(Long id);

    boolean isHoliday(LocalDate date);
}
