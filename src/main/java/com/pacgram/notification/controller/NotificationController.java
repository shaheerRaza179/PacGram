package com.pacgram.notification.controller;

import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.notification.dto.NotificationDto;
import com.pacgram.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications")
    public ApiResponse<Page<NotificationDto>> list(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(notificationService.list(principal, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.success(Map.of("count", notificationService.unreadCount(principal)));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ApiResponse<String> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal) {
        notificationService.markRead(id, principal);
        return ApiResponse.success("Notification marked as read.");
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResponse<String> markAllRead(@AuthenticationPrincipal AccountPrincipal principal) {
        notificationService.markAllRead(principal);
        return ApiResponse.success("Notifications marked as read.");
    }
}
