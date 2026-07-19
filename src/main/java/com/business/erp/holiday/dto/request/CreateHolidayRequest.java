package com.business.erp.holiday.dto.request;

import com.business.erp.holiday.entity.Holiday;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateHolidayRequest {
    @NotNull
    private LocalDate holidayDate;
    @NotBlank
    @Size(max = 200)
    private String name;
    @NotNull
    private Holiday.HolidayType holidayType;
}
