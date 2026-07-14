package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.TopJournalResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopKeywordResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.service.PublicationTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicationTrendServiceImpl implements PublicationTrendService {

    private final PaperRepository paperRepository;

    @Override
    @Transactional(readOnly = true)
    public List<YearCountResponse> getTrendChartData(String keyword, String author, String journal, Integer fromYear, Integer toYear, String institution, List<String> types, Boolean isOpenAccess, Integer fieldId, Integer topicId) {
        String kwParam = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String authParam = (author == null || author.isBlank()) ? null : author.trim();
        String jParam = (journal == null || journal.isBlank()) ? null : journal.trim();
        String instParam = (institution == null || institution.isBlank()) ? null : institution.trim();
        List<String> tParam = (types == null || types.isEmpty()) ? null : types;

        return paperRepository.countPapersByYearWithFilters(keyword, author, journal, fromYear, toYear, institution, types, null, fieldId, topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopKeywordResponse> getTopKeywords() {
        return paperRepository.findTopKeywords(PageRequest.of(0, 10));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopJournalResponse> getTopJournals(Integer fieldId) {
        return paperRepository.findTopJournalsByPaperCount(fieldId, PageRequest.of(0, 10));
    }
}
