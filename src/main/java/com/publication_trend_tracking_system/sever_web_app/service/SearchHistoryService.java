package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SearchHistoryResponse;

import java.util.List;

public interface SearchHistoryService {
    
    void saveSearchHistory(String keyword, String email);
    
    List<SearchHistoryResponse> getRecentSearchHistory(String email);
    
    List<PaperResponse> getSuggestionsBasedOnHistory(String email);
    
    void deleteSearchHistory(Long historyId, String email);
}
