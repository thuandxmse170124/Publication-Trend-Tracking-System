package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.NotificationResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicTrendResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.*;
import com.publication_trend_tracking_system.sever_web_app.enums.NotificationType;
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
                                .type(
                                        notification.getType())
                                .relatedCount(
                                        notification.getRelatedCount())
                                .build())
                .toList();
    }

    @Override
    public org.springframework.data.domain.Page<NotificationResponse> getMyNotifications(
            String email,
            org.springframework.data.domain.Pageable pageable) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository
                .findByUserUserIdOrderByCreatedAtDesc(user.getUserId(), pageable)
                .map(this::toResponse);
    }

    @Override
    public long countUnread(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return notificationRepository.countByUserUserIdAndIsReadFalse(user.getUserId());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .relatedId(notification.getRelatedId())
                .type(notification.getType())
                .relatedCount(notification.getRelatedCount())
                .build();
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
                                .type(
                                        notification.getType())
                                .relatedCount(
                                        notification.getRelatedCount())
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

        // Looked up in batches: a sizeable sync touches far more than 2,100 distinct authors, and
        // SQL Server refuses a parameterised IN-list longer than that. Sending the whole set in one
        // query failed the entire notification pass — and with it the job — even though every paper
        // had already been saved successfully.
        java.util.Map<Integer, List<FollowTopic>> topicFollowers =
                findInBatches(allTopicIds, followTopicRepository::findByTopicTopicIdIn).stream()
                        .collect(java.util.stream.Collectors.groupingBy(f -> f.getTopic().getTopicId()));

        java.util.Map<Long, List<FollowAuthor>> authorFollowers =
                findInBatches(allAuthorIds, followAuthorRepository::findByAuthorAuthorIdIn).stream()
                        .collect(java.util.stream.Collectors.groupingBy(f -> f.getAuthor().getAuthorId()));

        java.util.Map<Integer, List<FollowJournal>> journalFollowers =
                findInBatches(allJournalIds, followJournalRepository::findByJournalJournalIdIn).stream()
                        .collect(java.util.stream.Collectors.groupingBy(f -> f.getJournal().getJournalId()));

        // Accumulate per (user, follow target) instead of emitting a row per paper. One sync that
        // adds hundreds of papers to a followed topic previously wrote hundreds of near-identical
        // rows per follower; it now writes one that says how many.
        java.util.Map<AggregationKey, Integer> paperCounts = new java.util.LinkedHashMap<>();
        java.util.Map<AggregationKey, String> targetNames = new java.util.HashMap<>();
        java.util.Map<AggregationKey, User> recipients = new java.util.HashMap<>();

        for (Paper paper : newPapers) {
            // A user following several things that all match this paper is still one interested
            // reader, so each paper counts once per user. The first matching follow decides which
            // target the paper is attributed to, in topic → author → journal order.
            Set<Long> countedUsers = new HashSet<>();

            if (paper.getTopics() != null) {
                for (Topic topic : paper.getTopics()) {
                    for (FollowTopic follow : topicFollowers.getOrDefault(topic.getTopicId(), java.util.Collections.emptyList())) {
                        record(paperCounts, targetNames, recipients, countedUsers, follow.getUser(),
                                NotificationType.NEW_PAPERS_IN_TOPIC,
                                topic.getTopicId().longValue(), topic.getTopicName());
                    }
                }
            }

            if (paper.getAuthors() != null) {
                for (Author author : paper.getAuthors()) {
                    for (FollowAuthor follow : authorFollowers.getOrDefault(author.getAuthorId(), java.util.Collections.emptyList())) {
                        record(paperCounts, targetNames, recipients, countedUsers, follow.getUser(),
                                NotificationType.NEW_PAPERS_BY_AUTHOR,
                                author.getAuthorId(), author.getFullName());
                    }
                }
            }

            if (paper.getJournal() != null) {
                for (FollowJournal follow : journalFollowers.getOrDefault(paper.getJournal().getJournalId(), java.util.Collections.emptyList())) {
                    record(paperCounts, targetNames, recipients, countedUsers, follow.getUser(),
                            NotificationType.NEW_PAPERS_IN_JOURNAL,
                            paper.getJournal().getJournalId().longValue(), paper.getJournal().getName());
                }
            }
        }

        List<Notification> notificationsToSave = new java.util.ArrayList<>();
        for (java.util.Map.Entry<AggregationKey, Integer> entry : paperCounts.entrySet()) {
            AggregationKey key = entry.getKey();
            int count = entry.getValue();
            String name = targetNames.getOrDefault(key, "");

            notificationsToSave.add(Notification.builder()
                    .title(count == 1 ? "1 new paper" : count + " new papers")
                    .message(count == 1
                            ? "1 new paper in " + name
                            : count + " new papers in " + name)
                    .user(recipients.get(key))
                    .type(key.type())
                    .relatedId(key.targetId())
                    .relatedCount(count)
                    .build());
        }

        if (!notificationsToSave.isEmpty()) {
            notificationRepository.saveAll(notificationsToSave);
            log.info("Saved {} aggregated notifications for {} new papers",
                    notificationsToSave.size(), newPapers.size());
        }
    }

    // SQL Server caps a parameterised IN-list at 2,100 entries.
    private static final int ID_BATCH_SIZE = 1000;

    /** Runs an IN-list lookup in chunks small enough for the database to accept. */
    private <ID, T> List<T> findInBatches(
            Set<ID> ids,
            java.util.function.Function<java.util.Collection<ID>, List<T>> finder) {

        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<ID> ordered = new java.util.ArrayList<>(ids);
        List<T> results = new java.util.ArrayList<>();
        for (int start = 0; start < ordered.size(); start += ID_BATCH_SIZE) {
            results.addAll(finder.apply(
                    ordered.subList(start, Math.min(start + ID_BATCH_SIZE, ordered.size()))));
        }
        return results;
    }

    /** Identifies one notification: a single recipient and the followed thing it is about. */
    private record AggregationKey(Long userId, NotificationType type, Long targetId) {}

    private void record(
            java.util.Map<AggregationKey, Integer> paperCounts,
            java.util.Map<AggregationKey, String> targetNames,
            java.util.Map<AggregationKey, User> recipients,
            Set<Long> countedUsers,
            User user,
            NotificationType type,
            Long targetId,
            String targetName) {

        if (targetId == null || !countedUsers.add(user.getUserId())) {
            return;
        }
        AggregationKey key = new AggregationKey(user.getUserId(), type, targetId);
        paperCounts.merge(key, 1, Integer::sum);
        targetNames.putIfAbsent(key, targetName != null ? targetName : "");
        recipients.putIfAbsent(key, user);
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

        List<Notification> notificationsToSave = new java.util.ArrayList<>();

        for (FollowTopic follow : follows) {

            if (!userSubscriptionService.isPremium(
                    follow.getUser().getUserId())) {
                continue;
            }

            notificationsToSave.add(
                    Notification.builder()
                            .title("Topic Trend Alert")
                            .message(
                                    trend.getTopicName()
                                            + " is rapidly rising ("
                                            + String.format("%.1f",
                                            trend.getGrowthRate())
                                            + "% growth)")
                            .user(follow.getUser())
                            // Typed and linked, so this alert opens the topic instead of being a
                            // dead entry in the feed.
                            .type(NotificationType.TOPIC_TREND)
                            .relatedId(trend.getTopicId() != null
                                    ? trend.getTopicId().longValue() : null)
                            .build());
        }

        // One batched insert instead of a save() per follower: this runs once per rapidly-rising
        // topic at the tail of a sync job, where per-row inserts added up fast.
        if (!notificationsToSave.isEmpty()) {
            notificationRepository.saveAll(notificationsToSave);
        }
    }
}