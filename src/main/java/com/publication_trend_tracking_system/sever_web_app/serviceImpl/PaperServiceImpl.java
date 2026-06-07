package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.ApiSource;
import com.publication_trend_tracking_system.sever_web_app.entity.Journal;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.entity.ResearchField;
import com.publication_trend_tracking_system.sever_web_app.repository.ApiSourceRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.JournalRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.ResearchFieldRepository;
import com.publication_trend_tracking_system.sever_web_app.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final ResearchFieldRepository researchFieldRepository;
    private final ApiSourceRepository apiSourceRepository;

    @Override
    @Transactional
    public PaperResponse createPaper(PaperRequest request) {
        validateRequest(request, null);

        Paper paper = Paper.builder()
                .journal(getJournal(request.getJournalId()))
                .field(getResearchField(request.getFieldId()))
                .apiSource(getApiSource(request.getApiSourceId()))
                .publicationType(request.getPublicationType())
                .title(request.getTitle())
                .paperAbstract(request.getPaperAbstract())
                .publicationYear(request.getPublicationYear())
                .doi(normalize(request.getDoi()))
                .sourceUrl(request.getSourceUrl())
                .citationCount(request.getCitationCount())
                .visibilityStatus(request.getVisibilityStatus())
                .build();

        return toResponse(paperRepository.save(paper));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponse> getAllPapers(String keyword) {
        List<Paper> papers = (keyword == null || keyword.isBlank())
                ? paperRepository.findAllByOrderByCreatedAtDesc()
                : paperRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword.trim());

        return papers.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaperResponse getPaperById(Long paperId) {
        return toResponse(findPaper(paperId));
    }

    @Override
    @Transactional
    public PaperResponse updatePaper(Long paperId, PaperRequest request) {
        Paper existingPaper = findPaper(paperId);
        validateRequest(request, paperId);

        existingPaper.setJournal(getJournal(request.getJournalId()));
        existingPaper.setField(getResearchField(request.getFieldId()));
        existingPaper.setApiSource(getApiSource(request.getApiSourceId()));
        existingPaper.setPublicationType(request.getPublicationType());
        existingPaper.setTitle(request.getTitle());
        existingPaper.setPaperAbstract(request.getPaperAbstract());
        existingPaper.setPublicationYear(request.getPublicationYear());
        existingPaper.setDoi(normalize(request.getDoi()));
        existingPaper.setSourceUrl(request.getSourceUrl());
        existingPaper.setCitationCount(request.getCitationCount());
        existingPaper.setVisibilityStatus(request.getVisibilityStatus());

        return toResponse(paperRepository.save(existingPaper));
    }

    @Override
    @Transactional
    public void deletePaper(Long paperId) {
        paperRepository.delete(findPaper(paperId));
    }

    private void validateRequest(PaperRequest request, Long paperId) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paper title is required");
        }

        if (request.getPublicationType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publication type is required");
        }

        if (request.getVisibilityStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visibility status is required");
        }

        if (request.getCitationCount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Citation count is required");
        }

        String doi = normalize(request.getDoi());
        if (doi != null && !doi.isBlank()) {
            boolean duplicated = paperRepository.existsByDoi(doi);

            if (duplicated) {
                if (paperId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOI already exists");
                }

                Paper paper = findPaper(paperId);
                if (!doi.equalsIgnoreCase(normalize(paper.getDoi()))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOI already exists");
                }
            }
        }
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found"));
    }

    private Journal getJournal(Integer journalId) {
        if (journalId == null) {
            return null;
        }

        return journalRepository.findById(journalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal not found"));
    }

    private ResearchField getResearchField(Integer fieldId) {
        if (fieldId == null) {
            return null;
        }

        return researchFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Research field not found"));
    }

    private ApiSource getApiSource(Integer sourceId) {
        if (sourceId == null) {
            return null;
        }

        return apiSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API source not found"));
    }

    private PaperResponse toResponse(Paper paper) {
        return PaperResponse.builder()
                .paperId(paper.getPaperId())
                .journalId(paper.getJournal() != null ? paper.getJournal().getJournalId() : null)
                .journalName(paper.getJournal() != null ? paper.getJournal().getName() : null)
                .fieldId(paper.getField() != null ? paper.getField().getFieldId() : null)
                .fieldName(paper.getField() != null ? paper.getField().getFieldName() : null)
                .apiSourceId(paper.getApiSource() != null ? paper.getApiSource().getSourceId() : null)
                .apiSourceName(paper.getApiSource() != null ? paper.getApiSource().getSourceName() : null)
                .publicationType(paper.getPublicationType())
                .title(paper.getTitle())
                .paperAbstract(paper.getPaperAbstract())
                .publicationYear(paper.getPublicationYear())
                .doi(paper.getDoi())
                .sourceUrl(paper.getSourceUrl())
                .citationCount(paper.getCitationCount())
                .visibilityStatus(paper.getVisibilityStatus())
                .createdAt(paper.getCreatedAt())
                .updatedAt(paper.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
