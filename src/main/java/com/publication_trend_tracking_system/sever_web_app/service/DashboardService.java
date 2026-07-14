package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PersonalStatsResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PersonalizedDashboardResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SystemStatsResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicTrendResponse;


import java.util.List;

public interface DashboardService {
    SystemStatsResponse getSystemStats();
    PersonalStatsResponse getPersonalStats();

    PersonalizedDashboardResponse getPersonalizedDashboard();
    List<TopicTrendResponse> getTopicTrends();

}
