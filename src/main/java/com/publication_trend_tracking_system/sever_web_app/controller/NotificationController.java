package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
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

    @PutMapping("/{notificationId}/read")
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
}