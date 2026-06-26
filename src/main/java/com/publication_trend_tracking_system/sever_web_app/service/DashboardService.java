package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PersonalStatsResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SystemStatsResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse;

import java.util.List;

public interface DashboardService {
    SystemStatsResponse getSystemStats();
    PersonalStatsResponse getPersonalStats();
    List<YearCountResponse> getTrendChartData(String keyword, String author, String journal, Integer fromYear, Integer toYear, String institution, List<String> types, Boolean isOpenAccess, Integer fieldId, Integer topicId);
}
