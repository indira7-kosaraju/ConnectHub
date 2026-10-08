package com.connecthub.notification;

import com.connecthub.auth.AuthUser;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.CursorPage;
import com.connecthub.notification.dto.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;

    @Operation(summary = "Your notifications, newest first. Live ones also arrive on /user/queue/notifications.")
    @GetMapping
    public ApiResponse<CursorPage<NotificationDto>> list(@AuthenticationPrincipal AuthUser me,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(notifications.list(me.id(), cursor, limit));
    }

    @Operation(summary = "Unread notification count (the bell badge)")
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unread(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(Map.of("count", notifications.unreadCount(me.id())));
    }

    @Operation(summary = "Mark one notification read")
    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        notifications.markRead(me.id(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Mark all notifications read")
    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> readAll(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(Map.of("updated", notifications.markAllRead(me.id())));
    }
}
