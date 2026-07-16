package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.JournalResponse;

public interface JournalService {
    JournalResponse getJournalById(Integer journalId);
    org.springframework.data.domain.Page<JournalResponse> getAllJournals(org.springframework.data.domain.Pageable pageable);
}
