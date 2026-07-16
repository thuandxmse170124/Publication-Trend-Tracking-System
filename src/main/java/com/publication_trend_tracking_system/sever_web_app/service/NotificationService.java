package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.NotificationResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicTrendResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse>
    getMyNotifications(
            String email);

    void markAsRead(
            Long notificationId,
            String email);

    List<NotificationResponse>
    getUnreadNotifications(
            String email);

    void markAllAsRead(
            String email);

    void deleteNotification(
            Long notificationId,
            String email);

    void deleteAllNotifications(
            String email);

    void notifyUsersForNewPapers(
            List<Paper> newPapers);

    void notifyUsersForTrendingTopic(
            TopicTrendResponse trend);
}