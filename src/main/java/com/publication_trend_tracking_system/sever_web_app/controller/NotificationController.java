package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/notifications")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService
            notificationService;

    @GetMapping
    public ApiResponse<?> getMyNotifications(
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        notificationService
                                .getMyNotifications(
                                        authentication.getName()))
                .build();
    }

    // Paged variant. The unpaged endpoint above is kept so existing clients keep working, but this
    // is what the UI should call: a user's feed grows with every sync and there is no reason to
    // ship all of it to render a dropdown.
    @GetMapping("/paged")
    public ApiResponse<?> getMyNotificationsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(notificationService.getMyNotifications(
                        authentication.getName(),
                        org.springframework.data.domain.PageRequest.of(page, Math.min(size, 100))))
                .build();
    }

    // Counted in the database, so the badge stays correct regardless of how many pages exist.
    @GetMapping("/unread-count")
    public ApiResponse<?> getUnreadCount(Authentication authentication) {
        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(notificationService.countUnread(authentication.getName()))
                .build();
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<?> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {

        notificationService.markAsRead(
                notificationId,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message(
                        "Notification marked as read")
                .build();
    }

    @GetMapping("/unread")
    public ApiResponse<?> getUnreadNotifications(
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        notificationService
                                .getUnreadNotifications(
                                        authentication.getName()))
                .build();
    }

    @PatchMapping("/read-all")
    public ApiResponse<?> markAllAsRead(
            Authentication authentication) {

        notificationService
                .markAllAsRead(
                        authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message(
                        "All notifications marked as read")
                .build();
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<?> deleteNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {

        notificationService.deleteNotification(
                notificationId,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message(
                        "Notification deleted")
                .build();
    }

    @DeleteMapping
    public ApiResponse<?> deleteAllNotifications(
            Authentication authentication) {

        notificationService
                .deleteAllNotifications(
                        authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message(
                        "All notifications deleted")
                .build();
    }
}