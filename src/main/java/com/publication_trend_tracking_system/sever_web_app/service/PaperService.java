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
            Boolean isOpenAccess,
            Integer fieldId,
            Integer topicId,
            /** Also scan paper abstracts. Off by default — see PaperRepository.searchPapers. */
            boolean searchAbstract,
            Pageable pageable
    );

    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterKeywords(String search);
    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterJournals(String search);
    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterYears(String search);
    List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterTopics(String search);
}
