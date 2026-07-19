package com.business.erp.notification.controller;

import com.business.erp.auth.repository.UserRepository;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.notification.dto.NotificationResponse;
import com.business.erp.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "View and manage in-app notifications for the authenticated user")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @GetMapping
    @Operation(summary = "Get all notifications", description = "Returns all notifications for the authenticated user")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll(@AuthenticationPrincipal UserDetails u) {
        Long userId = getUserId(u.getUsername());
        log.debug("NotificationController:getAll :: userId={}", userId);
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getForUser(userId)));
    }

    @GetMapping("/count")
    @Operation(summary = "Get unread count", description = "Returns unread notification count for the authenticated user")
    public ResponseEntity<ApiResponse<Map<String, Long>>> count(@AuthenticationPrincipal UserDetails u) {
        Long userId = getUserId(u.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unread", notificationService.getUnreadCount(userId))));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        log.debug("NotificationController:markRead :: id={}", id);
        notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read"));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal UserDetails u) {
        Long userId = getUserId(u.getUsername());
        log.info("NotificationController:markAllRead :: userId={}", userId);
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }

    private Long getUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username)).getId();
    }
}
