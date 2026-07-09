package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedDashboardResponse {

    private List<FollowTopicResponse> followedTopics;

    private List<TopicTrendResponse> topicTrends;

    private List<PaperResponse> recentPapers;
}