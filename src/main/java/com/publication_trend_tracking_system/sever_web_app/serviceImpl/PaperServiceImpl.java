package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.*;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.*;
import com.publication_trend_tracking_system.sever_web_app.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final ResearchFieldRepository researchFieldRepository;
    private final ApiSourceRepository apiSourceRepository;
    private final AuthorRepository authorRepository;
    private final KeywordRepository keywordRepository;
    private final TopicRepository topicRepository;
    private final com.publication_trend_tracking_system.sever_web_app.service.SyncService syncService;

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
                .isOpenAccess(request.getIsOpenAccess())
                .build();

        resolveRelationships(paper, request);

        return toResponse(paperRepository.save(paper));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponse> getAllPapers(String keyword) {
        List<Paper> papers = (keyword == null || keyword.isBlank())
                ? paperRepository.findTop100ByOrderByCreatedAtDesc()
                : paperRepository.findTop100ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword.trim());

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
        existingPaper.setIsOpenAccess(request.getIsOpenAccess());

        resolveRelationships(existingPaper, request);

        return toResponse(paperRepository.save(existingPaper));
    }

    @Override
    @Transactional
    public void deletePaper(Long paperId) {
        paperRepository.delete(findPaper(paperId));
    }

    @Override
    @Transactional
    public Page<PaperResponse> searchPapers(
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
            Pageable pageable) {

        String kwParam = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String authParam = (author == null || author.isBlank()) ? null : author.trim();
        String jParam = (journal == null || journal.isBlank()) ? null : journal.trim();
        String instParam = (institution == null || institution.isBlank()) ? null : institution.trim();
        List<String> tParam = (types == null || types.isEmpty()) ? null : types;

        Page<Paper> papers = paperRepository.searchPapers(kwParam, authParam, jParam, fromYear, toYear, instParam, tParam, isOpenAccess, fieldId, topicId, pageable);

        // Advanced On-demand Sync: If keyword search returned 0 results, trigger an on-demand sync for this keyword
        if (papers.isEmpty() && kwParam != null) {
            try {
                // Find first active API source dynamically
                ApiSource activeSource = apiSourceRepository.findByStatusIgnoreCase("ACTIVE").orElse(null);
                if (activeSource != null) {
                    syncService.syncFromSource(activeSource.getSourceId(), null, kwParam);
                    // Query database again after sync completes
                    papers = paperRepository.searchPapers(kwParam, authParam, jParam, fromYear, toYear, instParam, tParam, isOpenAccess, fieldId, topicId, pageable);
                }
            } catch (Exception e) {
                log.error("Failed to run on-demand sync for keyword: " + kwParam, e);
            }
        }
        return papers.map(this::toResponse);
    }

    private void resolveRelationships(Paper paper, PaperRequest request) {
        // Resolve Authors
        Set<Author> authors = new HashSet<>();
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            authors.addAll(authorRepository.findAllById(request.getAuthorIds()));
        }
        paper.setAuthors(authors);

        // Resolve Keywords
        Set<Keyword> keywords = new HashSet<>();
        if (request.getKeywords() != null) {
            for (String kwName : request.getKeywords()) {
                if (kwName == null || kwName.isBlank()) continue;
                String trimmed = kwName.trim();
                Keyword keyword = keywordRepository.findByKeywordNameIgnoreCase(trimmed)
                        .orElseGet(() -> keywordRepository.save(Keyword.builder().keywordName(trimmed).build()));
                keywords.add(keyword);
            }
        }
        paper.setKeywords(keywords);

        // Resolve Topics
        Set<Topic> topics = new HashSet<>();
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            topics.addAll(topicRepository.findAllById(request.getTopicIds()));
        }
        paper.setTopics(topics);
    }

    private void validateRequest(PaperRequest request, Long paperId) {
        String doi = normalize(request.getDoi());
        if (doi != null && !doi.isBlank()) {
            boolean duplicated = paperRepository.existsByDoi(doi);

            if (duplicated) {
                if (paperId == null) {
                    throw new AppException(ErrorCode.DOI_EXISTED);
                }

                Paper paper = findPaper(paperId);
                if (!doi.equalsIgnoreCase(normalize(paper.getDoi()))) {
                    throw new AppException(ErrorCode.DOI_EXISTED);
                }
            }
        }
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new AppException(ErrorCode.PAPER_NOT_FOUND));
    }

    private Journal getJournal(Integer journalId) {
        if (journalId == null) {
            return null;
        }

        return journalRepository.findById(journalId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNAL_NOT_FOUND));
    }

    private ResearchField getResearchField(Integer fieldId) {
        if (fieldId == null) {
            return null;
        }

        return researchFieldRepository.findById(fieldId)
                .orElseThrow(() -> new AppException(ErrorCode.FIELD_NOT_FOUND));
    }

    private ApiSource getApiSource(Integer sourceId) {
        if (sourceId == null) {
            return null;
        }

        return apiSourceRepository.findById(sourceId)
                .orElseThrow(() -> new AppException(ErrorCode.API_SOURCE_NOT_FOUND));
    }

    private PaperResponse toResponse(Paper paper) {
        List<AuthorResponse> authorResponses = paper.getAuthors().stream()
                .map(author -> AuthorResponse.builder()
                        .authorId(author.getAuthorId())
                        .fullName(author.getFullName())
                        .affiliation(author.getAffiliation())
                        .orcid(author.getOrcid())
                        .build())
                .toList();

        List<String> keywordStrings = paper.getKeywords().stream()
                .map(Keyword::getKeywordName)
                .toList();

        List<String> topicStrings = paper.getTopics().stream()
                .map(Topic::getTopicName)
                .toList();

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
                .isOpenAccess(paper.getIsOpenAccess())
                .createdAt(paper.getCreatedAt())
                .updatedAt(paper.getUpdatedAt())
                .authors(authorResponses)
                .keywords(keywordStrings)
                .topics(topicStrings)
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterKeywords() {
        return keywordRepository.findTop50KeywordNamesWithCount().stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label((String) obj[0])
                        .value((String) obj[0])
                        .count(((Number) obj[1]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterJournals() {
        return journalRepository.findTop50JournalNamesWithCount().stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label((String) obj[0])
                        .value((String) obj[0])
                        .count(((Number) obj[1]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterYears() {
        return paperRepository.findDistinctYearsWithCount().stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label(String.valueOf(obj[0]))
                        .value(String.valueOf(obj[0]))
                        .count(((Number) obj[1]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterTopics() {
        return topicRepository.findTop50TopicsWithCount().stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label((String) obj[1])
                        .value(String.valueOf(obj[0]))
                        .count(((Number) obj[2]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}
