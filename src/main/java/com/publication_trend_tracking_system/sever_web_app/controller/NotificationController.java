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