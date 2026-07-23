package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.NotificationResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicTrendResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.*;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.*;
import com.publication_trend_tracking_system.sever_web_app.service.NotificationService;
import com.publication_trend_tracking_system.sever_web_app.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository
            notificationRepository;

    private final UserRepository
            userRepository;

    private final FollowTopicRepository
            followTopicRepository;

    private final FollowAuthorRepository
            followAuthorRepository;

    private final FollowJournalRepository
            followJournalRepository;

    private final UserSubscriptionService
            userSubscriptionService;

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
                                .relatedId(
                                        notification.getRelatedId())
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

    @Override
    public List<NotificationResponse>
    getUnreadNotifications(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return notificationRepository
                .findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(
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
                                .relatedId(
                                        notification.getRelatedId())
                                .build())
                .toList();
    }

    @Override
    public void markAllAsRead(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        List<Notification> notifications =
                notificationRepository
                        .findByUserUserIdOrderByCreatedAtDesc(
                                user.getUserId());

        notifications.forEach(
                notification ->
                        notification.setIsRead(true));

        notificationRepository.saveAll(
                notifications);
    }

    @Override
    public void deleteNotification(
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

        notificationRepository.delete(
                notification);
    }

    @Override
    public void deleteAllNotifications(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        notificationRepository
                .deleteByUserUserId(
                        user.getUserId());
    }

    @Transactional
    @Override
    public void notifyUsersForNewPapers(
            List<Paper> newPapers) {

        log.info(
                "Processing {} new papers",
                newPapers.size());

        Set<Integer> allTopicIds = new HashSet<>();
        Set<Long> allAuthorIds = new HashSet<>();
        Set<Integer> allJournalIds = new HashSet<>();

        for (Paper paper : newPapers) {
            if (paper.getTopics() != null) {
                for (Topic t : paper.getTopics()) allTopicIds.add(t.getTopicId());
            }
            if (paper.getAuthors() != null) {
                for (Author a : paper.getAuthors()) allAuthorIds.add(a.getAuthorId());
            }
            if (paper.getJournal() != null) {
                allJournalIds.add(paper.getJournal().getJournalId());
            }
        }

        java.util.Map<Integer, List<FollowTopic>> topicFollowers = allTopicIds.isEmpty() ? java.util.Collections.emptyMap() : 
            followTopicRepository.findByTopicTopicIdIn(allTopicIds).stream()
            .collect(java.util.stream.Collectors.groupingBy(f -> f.getTopic().getTopicId()));

        java.util.Map<Long, List<FollowAuthor>> authorFollowers = allAuthorIds.isEmpty() ? java.util.Collections.emptyMap() :
            followAuthorRepository.findByAuthorAuthorIdIn(allAuthorIds).stream()
            .collect(java.util.stream.Collectors.groupingBy(f -> f.getAuthor().getAuthorId()));

        java.util.Map<Integer, List<FollowJournal>> journalFollowers = allJournalIds.isEmpty() ? java.util.Collections.emptyMap() :
            followJournalRepository.findByJournalJournalIdIn(allJournalIds).stream()
            .collect(java.util.stream.Collectors.groupingBy(f -> f.getJournal().getJournalId()));

        List<Notification> notificationsToSave = new java.util.ArrayList<>();

        for (Paper paper : newPapers) {
            Set<Long> notifiedUsers = new HashSet<>();

            if (paper.getTopics() != null) {
                for (Topic topic : paper.getTopics()) {
                    List<FollowTopic> follows = topicFollowers.getOrDefault(topic.getTopicId(), java.util.Collections.emptyList());
                    for (FollowTopic follow : follows) {
                        Long userId = follow.getUser().getUserId();
                        if (notifiedUsers.add(userId)) {
                            notificationsToSave.add(Notification.builder()
                                    .title("New Paper Available")
                                    .message("New paper: " + paper.getTitle())
                                    .user(follow.getUser())
                                    .relatedId(paper.getPaperId())
                                    .build());
                        }
                    }
                }
            }

            if (paper.getAuthors() != null) {
                for (Author author : paper.getAuthors()) {
                    List<FollowAuthor> follows = authorFollowers.getOrDefault(author.getAuthorId(), java.util.Collections.emptyList());
                    for (FollowAuthor follow : follows) {
                        Long userId = follow.getUser().getUserId();
                        if (notifiedUsers.add(userId)) {
                            notificationsToSave.add(Notification.builder()
                                    .title("New Paper Available")
                                    .message("New paper: " + paper.getTitle())
                                    .user(follow.getUser())
                                    .relatedId(paper.getPaperId())
                                    .build());
                        }
                    }
                }
            }

            if (paper.getJournal() != null) {
                List<FollowJournal> follows = journalFollowers.getOrDefault(paper.getJournal().getJournalId(), java.util.Collections.emptyList());
                for (FollowJournal follow : follows) {
                    Long userId = follow.getUser().getUserId();
                    if (notifiedUsers.add(userId)) {
                        notificationsToSave.add(Notification.builder()
                                .title("New Paper Available")
                                .message("New paper: " + paper.getTitle())
                                .user(follow.getUser())
                                .relatedId(paper.getPaperId())
                                .build());
                    }
                }
            }
        }

        if (!notificationsToSave.isEmpty()) {
            notificationRepository.saveAll(notificationsToSave);
            log.info("Saved {} notifications in bulk", notificationsToSave.size());
        }
    }

    @Override
    @Transactional
    public void notifyUsersForTrendingTopic(
            TopicTrendResponse trend) {

        if (!"RAPIDLY_RISING".equals(trend.getTrend())) {
            return;
        }

        List<FollowTopic> follows =
                followTopicRepository.findByTopicTopicId(
                        trend.getTopicId());

        for (FollowTopic follow : follows) {

            if (!userSubscriptionService.isPremium(
                    follow.getUser().getUserId())) {
                continue;
            }

            Notification notification =
                    Notification.builder()
                            .title("Topic Trend Alert")
                            .message(
                                    trend.getTopicName()
                                            + " is rapidly rising ("
                                            + String.format("%.1f",
                                            trend.getGrowthRate())
                                            + "% growth)")
                            .user(follow.getUser())
                            .build();

            notificationRepository.save(notification);
        }
    }
}