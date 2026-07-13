package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.*;

import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.FollowTopicRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.DashboardService;
import com.publication_trend_tracking_system.sever_web_app.entity.FollowTopic;
import com.publication_trend_tracking_system.sever_web_app.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PaperRepository paperRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final FollowTopicRepository followTopicRepository;
    private final UserSubscriptionService userSubscriptionService;

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
                .findByUserUserId(
                        user.getUserId())
                .stream()
                .map(follow -> {

                    Integer topicId =
                            follow.getTopic()
                                    .getTopicId();

                    long paperCount =
                            paperRepository
                                    .countByTopics_TopicId(
                                            topicId);

                    String trend =
                            paperCount >= 100
                                    ? "RISING"
                                    : "STABLE";

                    return TopicTrendResponse
                            .builder()
                            .topicId(topicId)
                            .topicName(
                                    follow.getTopic()
                                            .getTopicName())
                            .paperCount(
                                    paperCount)
                            .trend(
                                    trend)
                            .build();
                })
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
                follows.stream()
                        .flatMap(follow ->
                                paperRepository
                                        .findTop10ByTopics_TopicIdOrderByCreatedAtDesc(
                                                follow.getTopic()
                                                        .getTopicId())
                                        .stream())
                        .limit(10)
                        .map(paper ->
                                PaperResponse.builder()
                                        .paperId(
                                                paper.getPaperId())
                                        .title(
                                                paper.getTitle())
                                        .publicationYear(
                                                paper.getPublicationYear())
                                        .doi(
                                                paper.getDoi())
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

    private void validatePremium(User user) {

        if (!userSubscriptionService.isPremium(user.getUserId())) {
            throw new AppException(
                    ErrorCode.PREMIUM_REQUIRED);
        }
    }
}
