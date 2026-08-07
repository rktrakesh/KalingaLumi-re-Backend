package com.business.erp.employee.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.employee.dto.response.EmployeeDocumentContent;
import com.business.erp.employee.dto.response.EmployeeDocumentListResponse;
import com.business.erp.employee.dto.response.EmployeeDocumentResponse;
import com.business.erp.employee.enums.EmployeeDocumentType;
import com.business.erp.employee.service.EmployeeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employee Documents", description = "Secure employee-document management and self-service access")
public class EmployeeDocumentController {

    private final EmployeeDocumentService documentService;

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_EMPLOYEE','ROLE_SALES')")
    public ResponseEntity<ApiResponse<EmployeeDocumentListResponse>> list(
            @PathVariable Long employeeId, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.listCurrent(employeeId, principal)));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR')")
    public ResponseEntity<ApiResponse<EmployeeDocumentResponse>> upload(
            @PathVariable Long employeeId,
            @RequestParam EmployeeDocumentType documentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                documentService.upload(employeeId, documentType, expiryDate, file, principal),
                "Employee document uploaded"));
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE','ROLE_SALES')")
    public ResponseEntity<ApiResponse<EmployeeDocumentResponse>> metadata(
            @PathVariable Long employeeId, @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                documentService.getMetadata(employeeId, documentId, principal)));
    }

    @GetMapping("/documents/{documentId}/content")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE','ROLE_SALES')")
    @Operation(summary = "View or download an employee document")
    public ResponseEntity<Resource> content(
            @PathVariable Long employeeId, @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean download,
            @AuthenticationPrincipal UserDetails principal) {
        EmployeeDocumentContent content = documentService.getContent(employeeId, documentId, principal);
        ContentDisposition disposition = (download
                ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(content.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR')")
    public ResponseEntity<ApiResponse<Void>> archive(
            @PathVariable Long employeeId, @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails principal) {
        documentService.archive(employeeId, documentId, principal);
        return ResponseEntity.ok(ApiResponse.ok((Void) null, "Employee document archived"));
    }

    @GetMapping("/profile-photo")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE','ROLE_SALES')")
    public ResponseEntity<org.springframework.core.io.Resource> profilePhoto(
            @PathVariable Long employeeId, @AuthenticationPrincipal UserDetails principal) {
        EmployeeDocumentContent content = documentService.getCurrentProfilePhoto(employeeId, principal);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(content.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }
}
