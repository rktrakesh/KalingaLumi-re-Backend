package com.business.erp.settings.controller;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.storage.FileStorageService;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/branding")
public class PublicSettingsController {
    private final SettingsService settings;
    private final FileStorageService storage;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> branding() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "companyName", settings.getCurrentValue(SettingKey.COMPANY_NAME),
                "companyShortName", settings.getCurrentValue(SettingKey.COMPANY_SHORT_NAME),
                "companyLogoUrl", settings.getCurrentValue(SettingKey.COMPANY_LOGO_URL))));
    }

    @GetMapping("/company-logo/{filename:.+}")
    public ResponseEntity<Resource> logo(@PathVariable String filename) {
        Resource resource = storage.loadLogo(filename);
        MediaType contentType = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(contentType).body(resource);
    }
}
