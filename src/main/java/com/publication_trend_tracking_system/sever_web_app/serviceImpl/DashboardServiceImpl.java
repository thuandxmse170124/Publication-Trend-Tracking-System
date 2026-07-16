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
import com.publication_trend_tracking_system.sever_web_app.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.publication_trend_tracking_system.sever_web_app.entity.BookmarkPaper;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.repository.BookmarkPaperRepository;

import java.text.DecimalFormat;
import java.util.ArrayList;
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

        long thisMonth = paperRepository.countPapersThisMonth();
        long lastMonth = paperRepository.countPapersLastMonth();
        
        String publicationTrend;
        if (lastMonth == 0) {
            publicationTrend = thisMonth > 0 ? "+100%" : "0%";
        } else {
            double percent = ((double) (thisMonth - lastMonth) / lastMonth) * 100;
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

        Set<Long> bookmarkedPaperIds =
                bookmarkPaperRepository
                        .findByUserUserId(user.getUserId())
                        .stream()
                        .map(BookmarkPaper::getPaperId)
                        .collect(Collectors.toSet());

        List<Paper> candidatePapers =
                new ArrayList<>(
                        follows.stream()
                                .flatMap(follow ->
                                        paperRepository
                                                .findByTopics_TopicId(
                                                        follow.getTopic().getTopicId())
                                                .stream())
                                .filter(paper ->
                                        !bookmarkedPaperIds.contains(
                                                paper.getPaperId()))
                                .collect(Collectors.toMap(
                                        Paper::getPaperId,
                                        paper -> paper,
                                        (first, second) -> first
                                ))
                                .values()
                );

        Collections.shuffle(candidatePapers);

        List<PaperResponse> recentPapers =
                candidatePapers.stream()
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

        Integer topicId = topic.getTopicId();

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime currentStart = now.minusDays(30);
        LocalDateTime previousStart = now.minusDays(60);

        long currentCount =
                paperRepository.countTopicPapersBetween(
                        topicId,
                        currentStart,
                        now);

        long previousCount =
                paperRepository.countTopicPapersBetween(
                        topicId,
                        previousStart,
                        currentStart);

        Double growthRate = null;
        Double trendScore = null;

        long paperCount = currentCount;

        String trend;

        if (currentCount == 0 && previousCount == 0) {

            trend = "NO_DATA";

        } else if (currentCount < 10) {

            trend = "LOW_ACTIVITY";

        } else if (previousCount == 0) {

            trend = "EMERGING";

        } else {

            growthRate =
                    ((double) (currentCount - previousCount)
                            / previousCount) * 100;

            trendScore = growthRate;

            if (growthRate >= 30) {
                trend = "RAPIDLY_RISING";
            } else if (growthRate >= 10) {
                trend = "RISING";
            } else if (growthRate <= -10) {
                trend = "DECLINING";
            } else {
                trend = "STABLE";
            }
        }

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

    @Override
    @Transactional(readOnly = true)
    public List<TopicTrendResponse> getAllTopicTrends() {

        return topicRepository.findAll()
                .stream()
                .map(this::calculateTrend)
                .toList();
    }

    private void validatePremium(User user) {

        if (!userSubscriptionService.isPremium(user.getUserId())) {
            throw new AppException(
                    ErrorCode.PREMIUM_REQUIRED);
        }
    }
}
