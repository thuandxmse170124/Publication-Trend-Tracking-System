package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.*;
import java.time.LocalDateTime;
import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.*;
import com.publication_trend_tracking_system.sever_web_app.service.DashboardService;
import com.publication_trend_tracking_system.sever_web_app.entity.FollowTopic;
import com.publication_trend_tracking_system.sever_web_app.service.NotificationService;
import com.publication_trend_tracking_system.sever_web_app.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.publication_trend_tracking_system.sever_web_app.entity.BookmarkPaper;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.repository.BookmarkPaperRepository;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PaperRepository paperRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final FollowTopicRepository followTopicRepository;
    private final UserSubscriptionService userSubscriptionService;
    private final BookmarkPaperRepository bookmarkPaperRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStats() {
        long totalPapers = paperRepository.count();
        
        List<Topic> topTopicsEntity = topicRepository.findTop5TrendingTopics();
        List<TopicResponse> topTopics = topTopicsEntity.stream()
                .map(t -> TopicResponse.builder()
                        .topicId(t.getTopicId())
                        .topicName(t.getTopicName())
                        .description(t.getDescription())
                        .build())
                .collect(Collectors.toList());

        // Year-over-year by publication_year, same basis as getAllTopicTrends()/calculateTrend()
        // below — comparing by created_at (our own sync schedule) instead would make this measure
        // sync activity, not real publication activity.
        int currentYear = java.time.LocalDate.now().getYear();
        int previousYear = currentYear - 1;
        long thisYear = paperRepository.countByPublicationYear(currentYear);
        long lastYear = paperRepository.countByPublicationYear(previousYear);

        String publicationTrend;
        if (lastYear < MIN_PREVIOUS_COUNT_FOR_GROWTH_RATE
                || lastYear < thisYear * MIN_PREVIOUS_RATIO_FOR_GROWTH_RATE) {
            publicationTrend = "N/A";
        } else {
            double percent = ((double) (thisYear - lastYear) / lastYear) * 100;
            DecimalFormat df = new DecimalFormat("+#,##0.0;-#");
            publicationTrend = df.format(percent) + "%";
        }

        return SystemStatsResponse.builder()
                .totalPapers(totalPapers)
                .topTopics(topTopics)
                .publicationTrend(publicationTrend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalStatsResponse getPersonalStats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        long bookmarks = userRepository.countBookmarksByUserId(user.getUserId());
        long followingTopics = userRepository.countFollowedTopicsByUserId(user.getUserId());
        long followingAuthors = userRepository.countFollowedAuthorsByUserId(user.getUserId());
        long followingJournals = userRepository.countFollowedJournalsByUserId(user.getUserId());
        long unreadNotifications = userRepository.countUnreadNotificationsByUserId(user.getUserId());
        List<String> recentSearches = userRepository.findRecentSearchesByUserId(user.getUserId());

        return PersonalStatsResponse.builder()
                .bookmarks(bookmarks)
                .followingTopics(followingTopics)
                .followingAuthors(followingAuthors)
                .followingJournals(followingJournals)
                .unreadNotifications(unreadNotifications)
                .recentSearches(recentSearches)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<TopicTrendResponse> getTopicTrends() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND));
        validatePremium(user);

        return followTopicRepository
                .findByUserUserId(user.getUserId())
                .stream()
                .map(follow ->
                        calculateTrend(follow.getTopic()))
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public PersonalizedDashboardResponse
    getPersonalizedDashboard() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND));
        validatePremium(user);

        List<FollowTopic> follows =
                followTopicRepository
                        .findByUserUserId(
                                user.getUserId());

        List<FollowTopicResponse> followedTopics =
                follows.stream()
                        .map(follow ->
                                FollowTopicResponse
                                        .builder()
                                        .followId(
                                                follow.getFollowId())
                                        .topicId(
                                                follow.getTopic()
                                                        .getTopicId())
                                        .topicName(
                                                follow.getTopic()
                                                        .getTopicName())
                                        .build())
                        .toList();

        List<TopicTrendResponse> topicTrends =
                getTopicTrends();

        List<PaperResponse> recentPapers =
                paperRepository.findRandomRecommendedPapersForUser(user.getUserId())
                        .stream()
                        .limit(10)
                        .map(paper ->
                                PaperResponse.builder()
                                        .paperId(paper.getPaperId())
                                        .title(paper.getTitle())
                                        .publicationYear(
                                                paper.getPublicationYear())
                                        .doi(paper.getDoi())
                                        .citationCount(
                                                paper.getCitationCount())
                                        .isOpenAccess(
                                                paper.getIsOpenAccess())
                                        .build())
                        .toList();

        return PersonalizedDashboardResponse
                .builder()
                .followedTopics(
                        followedTopics)
                .topicTrends(
                        topicTrends)
                .recentPapers(
                        recentPapers)
                .build();
    }

    private TopicTrendResponse calculateTrend(Topic topic) {
        int currentYear = LocalDateTime.now().getYear();
        int previousYear = currentYear - 1;

        long currentCount =
                paperRepository.countTopicPapersByYear(
                        topic.getTopicId(),
                        currentYear);

        long previousCount =
                paperRepository.countTopicPapersByYear(
                        topic.getTopicId(),
                        previousYear);

        return calculateTrend(topic, currentCount, previousCount);
    }

    // Below this many papers in the current year a topic has too little activity to classify at all.
    private static final int MIN_CURRENT_COUNT_FOR_TREND = 10;
    // Absolute floor: a base under 10 is too small to trust regardless of scale.
    private static final int MIN_PREVIOUS_COUNT_FOR_GROWTH_RATE = 10;
    // Relative floor: with sync always fetching newest papers first, a popular topic can have
    // currentCount in the thousands against a previousCount in the tens — an absolute floor alone
    // lets that through and still produces nonsense like +43350%. Require the base to be at least
    // 10% of the current count before trusting a percentage.
    private static final double MIN_PREVIOUS_RATIO_FOR_GROWTH_RATE = 0.1;

    /**
     * Returns null when the previous-year base is too small — in absolute terms or relative to the
     * current count — for a percentage change to mean anything. A tiny denominator produces an
     * artifact, not a trend signal.
     */
    private static Double growthRateOrNull(long currentCount, long previousCount) {
        if (previousCount < MIN_PREVIOUS_COUNT_FOR_GROWTH_RATE
                || previousCount < currentCount * MIN_PREVIOUS_RATIO_FOR_GROWTH_RATE) {
            return null;
        }
        return ((double) (currentCount - previousCount) / previousCount) * 100;
    }

    // Extracted so callers that only need the label — checkAndNotifyTrendingTopics filtering for
    // RAPIDLY_RISING — can classify from raw counts without loading a Topic entity first.
    private static String classifyTrend(long currentCount, long previousCount) {
        if (currentCount == 0 && previousCount == 0) {
            return "NO_DATA";
        }
        if (currentCount < MIN_CURRENT_COUNT_FOR_TREND) {
            return "LOW_ACTIVITY";
        }
        Double growthRate = growthRateOrNull(currentCount, previousCount);
        if (growthRate == null) {
            return "EMERGING";
        }
        if (growthRate >= 30) {
            return "RAPIDLY_RISING";
        }
        if (growthRate >= 10) {
            return "RISING";
        }
        if (growthRate <= -10) {
            return "DECLINING";
        }
        return "STABLE";
    }

    private TopicTrendResponse calculateTrend(Topic topic, long currentCount, long previousCount) {

        Integer topicId = topic.getTopicId();

        long paperCount = currentCount;

        String trend = classifyTrend(currentCount, previousCount);

        // Only the growth-classified labels carry a percentage; NO_DATA/LOW_ACTIVITY/EMERGING
        // deliberately report no rate so the UI shows "N/A" instead of a misleading number.
        Double growthRate = switch (trend) {
            case "RAPIDLY_RISING", "RISING", "DECLINING", "STABLE" ->
                    growthRateOrNull(currentCount, previousCount);
            default -> null;
        };
        Double trendScore = growthRate;

        return TopicTrendResponse.builder()
                .topicId(topicId)
                .topicName(topic.getTopicName())
                .paperCount(paperCount)
                .previousPaperCount(previousCount)
                .currentPaperCount(currentCount)
                .growthRate(growthRate)
                .trendScore(trendScore)
                .trend(trend)
                .build();
    }

    // Public trend view (Trend Analytics page): unlike getTopicTrends() above, this is not
    // premium-gated and is not limited to the current user's followed topics. Iterates only
    // topics that have at least one paper (getTopicTrendCounts INNER JOINs paper_topics), instead
    // of topicRepository.findAll() — with the full 4,516-topic official taxonomy now seeded,
    // findAll() would return thousands of all-zero rows that are pure noise on this page.
    private static final int ALL_TOPIC_TRENDS_LIMIT = 5;

    @Override
    @Transactional(readOnly = true)
    public List<TopicTrendResponse> getAllTopicTrends() {
        int currentYear = LocalDateTime.now().getYear();
        int previousYear = currentYear - 1;

        List<Object[]> counts = paperRepository.getTopicTrendCounts(previousYear, currentYear);
        java.util.Map<Integer, long[]> countMap = counts.stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).intValue(),
            row -> new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()}
        ));

        // Sort and cap to the top N *before* fetching Topic entities: SQL Server rejects an
        // IN-clause with more than 2100 parameters, and with the full taxonomy seeded, the number
        // of distinct topics that have at least one paper can exceed that.
        List<java.util.Map.Entry<Integer, long[]>> topEntries = countMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue()[0] + b.getValue()[1],
                        a.getValue()[0] + a.getValue()[1]))
                .limit(ALL_TOPIC_TRENDS_LIMIT)
                .toList();

        List<Integer> topicIds = topEntries.stream().map(java.util.Map.Entry::getKey).toList();
        java.util.Map<Integer, Topic> topicsById = topicRepository.findAllById(topicIds).stream()
                .collect(Collectors.toMap(Topic::getTopicId, t -> t));

        return topEntries.stream()
                .filter(entry -> topicsById.containsKey(entry.getKey()))
                .map(entry -> calculateTrend(topicsById.get(entry.getKey()), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    // Called by SyncServiceImpl right after a sync job saves new papers, for the exact set of
    // topics those papers belong to — this is the only place trend-alert notifications get
    // triggered, since trends are otherwise only computed on-demand for API responses.
    // SQL Server rejects a parameterised IN-list longer than 2100 entries, and a full sync can
    // touch far more topics than that, so entity loads here are chunked.
    private static final int TOPIC_ID_BATCH_SIZE = 1000;

    @Override
    @Transactional
    public void checkAndNotifyTrendingTopics(Set<Integer> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return;
        }

        int currentYear = LocalDateTime.now().getYear();
        int previousYear = currentYear - 1;

        // One aggregate query covers every topic's year-over-year counts. The previous version
        // looped calculateTrend(topic) instead, costing two COUNT queries per topic — a full sync
        // touching thousands of topics fired thousands of queries the moment the job finished,
        // which is what drained the connection pool. It also handed the whole id set to
        // findAllById() in one go, past SQL Server's 2100-parameter limit.
        java.util.Map<Integer, long[]> countsByTopicId = paperRepository
                .getTopicTrendCounts(previousYear, currentYear)
                .stream()
                .filter(row -> topicIds.contains(((Number) row[0]).intValue()))
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()}));

        // Classify from the counts alone, so only the topics that actually qualify get loaded.
        List<Integer> risingTopicIds = countsByTopicId.entrySet().stream()
                .filter(entry -> "RAPIDLY_RISING".equals(
                        classifyTrend(entry.getValue()[0], entry.getValue()[1])))
                .map(java.util.Map.Entry::getKey)
                .toList();

        for (int start = 0; start < risingTopicIds.size(); start += TOPIC_ID_BATCH_SIZE) {
            List<Integer> batch = risingTopicIds.subList(
                    start, Math.min(start + TOPIC_ID_BATCH_SIZE, risingTopicIds.size()));
            for (Topic topic : topicRepository.findAllById(batch)) {
                long[] counts = countsByTopicId.get(topic.getTopicId());
                notificationService.notifyUsersForTrendingTopic(
                        calculateTrend(topic, counts[0], counts[1]));
            }
        }
    }

    private void validatePremium(User user) {

        if (!userSubscriptionService.isPremium(user.getUserId())) {
            throw new AppException(
                    ErrorCode.PREMIUM_REQUIRED);
        }
    }
}
