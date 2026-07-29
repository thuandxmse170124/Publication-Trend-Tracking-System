package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.NotificationResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicTrendResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse>
    getMyNotifications(
            String email);

    /** Paged feed — the unpaged variant above loads a user's whole history at once. */
    org.springframework.data.domain.Page<NotificationResponse>
    getMyNotifications(
            String email,
            org.springframework.data.domain.Pageable pageable);

    /** Unread total for the badge, counted in the database rather than from a fetched page. */
    long countUnread(String email);

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