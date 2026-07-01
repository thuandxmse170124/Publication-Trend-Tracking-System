package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaperService {

    PaperResponse createPaper(PaperRequest request);

    List<PaperResponse> getAllPapers(String keyword);

    PaperResponse getPaperById(Long paperId);

    PaperResponse updatePaper(Long paperId, PaperRequest request);

    void deletePaper(Long paperId);

    Page<PaperResponse> searchPapers(
            String keyword,
            String author,
            String journal,
            Integer fromYear,
            Integer toYear,
            String institution,
            List<String> types,
            Integer fieldId,
            Integer topicId,
            Pageable pageable
    );

    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterKeywords();
    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterJournals();
    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterYears();
    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterTopics();
}
