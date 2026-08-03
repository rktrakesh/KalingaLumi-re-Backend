package com.business.erp.settings.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.settings.dto.response.SettingResponse;
import com.business.erp.settings.dto.request.UpdateSettingRequest;
import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.entity.AppSettingHistory;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.enums.SettingCategory;
import com.business.erp.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Settings", description = "Manage application-level configurable business rules (Admin only)")
public class SettingsController {

    private final SettingsService settingsService;
    private final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @GetMapping
    @Operation(summary = "Get all settings", description = "Retrieve all current application settings, optionally filtered by category")
    public ResponseEntity<ApiResponse<List<SettingResponse>>> getAll(
            @RequestParam(required = false) String category) {
        SettingCategory settingCategory = parseSettingCategory(category);
        log.debug("SettingsController:getAll :: Fetching settings category={}", settingCategory);
        List<SettingResponse> settings = settingsService.getAllSettings(settingCategory).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update a setting", description = "Update a setting value. Change takes effect from next payroll cycle start")
    public ResponseEntity<ApiResponse<SettingResponse>> update(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request,
            @AuthenticationPrincipal UserDetails user) {
        log.info("SettingsController:update :: key={} by={}", key, user.getUsername());
        SettingKey settingKey = parseSettingKey(key);
        AppSetting updated = settingsService.updateSetting(settingKey, request.getValue(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(updated), "Setting updated. Effective from next payroll cycle."));
    }

    @GetMapping("/history/{key}")
    @Operation(summary = "Get setting history", description = "Get historical changes for a specific setting key")
    public ResponseEntity<ApiResponse<List<AppSettingHistory>>> getHistory(@PathVariable String key) {
        log.debug("SettingsController:getHistory :: key={}", key);
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getHistory(parseSettingKey(key))));
    }

    private SettingResponse toResponse(AppSetting s) {
        return SettingResponse.builder()
                .id(s.getId()).settingKey(s.getSettingKey()).settingCategory(s.getSettingCategory().name()).settingValue(s.getSettingValue())
                .description(s.getDescription()).effectiveFromDate(s.getEffectiveFromDate()).build();
    }

    private SettingKey parseSettingKey(String key) {
        try {
            return SettingKey.valueOf(key);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid setting key: " + key);
        }
    }

    private SettingCategory parseSettingCategory(String category) {
        if (category == null || category.isBlank()) return null;
        try {
            return SettingCategory.valueOf(category);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid setting category: " + category);
        }
    }
}
