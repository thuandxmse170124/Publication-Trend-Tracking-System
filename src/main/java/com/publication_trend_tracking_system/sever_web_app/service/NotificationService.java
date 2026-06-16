package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse>
    getMyNotifications(
            String email);

    void markAsRead(
            Long notificationId,
            String email);
}