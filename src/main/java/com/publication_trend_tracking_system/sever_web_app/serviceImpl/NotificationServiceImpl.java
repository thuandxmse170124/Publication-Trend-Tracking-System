package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.NotificationResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Notification;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.NotificationRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository
            notificationRepository;

    private final UserRepository
            userRepository;

    @Override
    public List<NotificationResponse>
    getMyNotifications(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return notificationRepository
                .findByUserUserIdOrderByCreatedAtDesc(
                        user.getUserId())
                .stream()
                .map(notification ->
                        NotificationResponse.builder()
                                .notificationId(
                                        notification.getNotificationId())
                                .title(
                                        notification.getTitle())
                                .message(
                                        notification.getMessage())
                                .isRead(
                                        notification.getIsRead())
                                .createdAt(
                                        notification.getCreatedAt())
                                .build())
                .toList();
    }

    @Override
    public void markAsRead(
            Long notificationId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new AppException(
                    ErrorCode.UNAUTHORIZED);
        }

        if (Boolean.TRUE.equals(
                notification.getIsRead())) {

            throw new AppException(
                    ErrorCode.NOTIFICATION_ALREADY_READ);
        }

        notification.setIsRead(true);

        notificationRepository.save(
                notification);
    }
}