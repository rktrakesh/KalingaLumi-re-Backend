package com.business.erp.holiday.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.holiday.dto.request.CreateHolidayRequest;
import com.business.erp.holiday.dto.response.HolidayResponse;
import com.business.erp.holiday.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Holidays", description = "Manage factory and national holidays — auto-creates HOLIDAY attendance for all active employees")
public class HolidayController {

    private final HolidayService holidayService;
    private final Logger log = LoggerFactory.getLogger(HolidayController.class);

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create holiday", description = "Add a holiday and auto-create HOLIDAY attendance records for all active employees")
    public ResponseEntity<ApiResponse<HolidayResponse>> create(@Valid @RequestBody CreateHolidayRequest req) {
        log.info("HolidayController:create :: date={} name={}", req.getHolidayDate(), req.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(holidayService.create(req)));
    }

    @GetMapping
    @Operation(summary = "List holidays", description = "Get all holidays for a given year")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getByYear(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        log.debug("HolidayController:getByYear :: year={}", year);
        return ResponseEntity.ok(ApiResponse.ok(holidayService.getByYear(year)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete holiday", description = "Remove a holiday and revert HOLIDAY attendance back to ABSENT")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("HolidayController:delete :: id={}", id);
        holidayService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Holiday removed and attendance reverted"));
    }
}
