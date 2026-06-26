package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.TopJournalResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopKeywordResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse;

import java.util.List;

public interface PublicationTrendService {
    List<YearCountResponse> getTrendChartData(String keyword, String author, String journal, Integer fromYear, Integer toYear, String institution, List<String> types, Boolean isOpenAccess, Integer fieldId, Integer topicId);
    List<TopKeywordResponse> getTopKeywords();
    List<TopJournalResponse> getTopJournals(Integer fieldId);
}
