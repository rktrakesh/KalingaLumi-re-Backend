package com.business.erp.inventory.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import com.business.erp.inventory.dto.request.CreateMaterialRequest;
import com.business.erp.inventory.dto.response.InventoryLedgerResponse;
import com.business.erp.inventory.dto.response.MaterialResponse;
import com.business.erp.inventory.dto.request.StockAdjustmentRequest;
import com.business.erp.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventory", description = "Inventory operations")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/materials")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialResponse>> createMaterial(
            @Valid @RequestBody CreateMaterialRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(inventoryService.createMaterial(req)));
    }

    @GetMapping("/materials")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getAllMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllMaterials()));
    }

    @GetMapping("/materials/{id}/stock")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialResponse>> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getMaterial(id)));
    }

    @PostMapping("/inventory/adjustment")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryLedgerResponse>> adjust(
            @Valid @RequestBody StockAdjustmentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.adjust(req, user.getUsername()), "Stock adjusted"));
    }

    @GetMapping("/inventory/ledger/{materialId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryLedgerResponse>>> getLedger(
            @PathVariable Long materialId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLedger(materialId, pageable)));
    }

    @GetMapping("/inventory/low-stock")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockAlerts()));
    }
}
