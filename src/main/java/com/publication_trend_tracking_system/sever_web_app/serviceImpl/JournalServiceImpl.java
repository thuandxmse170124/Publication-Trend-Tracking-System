package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.JournalResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Journal;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.JournalRepository;
import com.publication_trend_tracking_system.sever_web_app.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;

    @Override
    public JournalResponse getJournalById(Integer journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNAL_NOT_FOUND));
        return toResponse(journal);
    }

    @Override
    public org.springframework.data.domain.Page<JournalResponse> getAllJournals(org.springframework.data.domain.Pageable pageable) {
        return journalRepository.findAll(pageable).map(this::toResponse);
    }

    private JournalResponse toResponse(Journal journal) {
        return JournalResponse.builder()
                .journalId(journal.getJournalId())
                .name(journal.getName())
                .issn(journal.getIssn())
                .publisher(journal.getPublisher())
                .status(journal.getStatus())
                .build();
    }
}
