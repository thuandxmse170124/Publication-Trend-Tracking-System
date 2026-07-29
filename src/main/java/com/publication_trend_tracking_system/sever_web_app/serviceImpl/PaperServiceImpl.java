package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicTagResponse;
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
    private final SyncJobRepository syncJobRepository;
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
        Paper paper = findPaper(paperId);
        Set<Long> authorIds = paper.getAuthors().stream().map(Author::getAuthorId).collect(java.util.stream.Collectors.toSet());
        Integer journalId = paper.getJournal() != null ? paper.getJournal().getJournalId() : null;

        paperRepository.delete(paper);
        // Flush so the DB's ON DELETE CASCADE on paper_authors/paper_journal has actually run
        // before we count remaining references below.
        paperRepository.flush();

        // Authors and journals are auto-created from sync data, not a fixed taxonomy (unlike
        // Topics) — once a paper is gone, an author/journal with no other papers left is just an
        // orphan row, so clean it up rather than leaving it to accumulate forever.
        for (Long authorId : authorIds) {
            if (paperRepository.countByAuthors_AuthorId(authorId) == 0) {
                authorRepository.deleteById(authorId);
            }
        }
        if (journalId != null && paperRepository.countByJournal_JournalId(journalId) == 0) {
            journalRepository.deleteById(journalId);
        }
    }

    @Override
    @Transactional(readOnly = true)
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
            boolean searchAbstract,
            Pageable pageable) {

        String kwParam = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String authParam = (author == null || author.isBlank()) ? null : author.trim();
        String jParam = (journal == null || journal.isBlank()) ? null : journal.trim();
        String instParam = (institution == null || institution.isBlank()) ? null : institution.trim();
        List<String> tParam = (types == null || types.isEmpty()) ? null : types;

        // Picking between two queries rather than passing a flag into one: a bind parameter cannot
        // be folded away by the planner, so a single combined query keeps the expensive abstract
        // scan in its plan even when the caller does not want it.
        Page<Paper> papers = searchAbstract
                ? paperRepository.searchPapersIncludingAbstract(kwParam, authParam, jParam, fromYear, toYear, instParam, tParam, isOpenAccess, fieldId, topicId, pageable)
                : paperRepository.searchPapers(kwParam, authParam, jParam, fromYear, toYear, instParam, tParam, isOpenAccess, fieldId, topicId, pageable);

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
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            java.util.Set<String> kwNames = request.getKeywords().stream()
                    .filter(k -> k != null && !k.isBlank())
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toSet());
                    
            if (!kwNames.isEmpty()) {
                java.util.List<Keyword> existing = keywordRepository.findAllByKeywordNameInIgnoreCase(kwNames);
                java.util.Map<String, Keyword> existingMap = new java.util.HashMap<>();
                for (Keyword k : existing) {
                    existingMap.put(k.getKeywordName().toLowerCase(), k);
                }
                
                java.util.List<Keyword> toSave = new java.util.ArrayList<>();
                for (String kwName : kwNames) {
                    String lower = kwName.toLowerCase();
                    if (existingMap.containsKey(lower)) {
                        keywords.add(existingMap.get(lower));
                    } else {
                        Keyword newK = Keyword.builder().keywordName(kwName).build();
                        toSave.add(newK);
                        keywords.add(newK);
                        existingMap.put(lower, newK);
                    }
                }
                if (!toSave.isEmpty()) {
                    keywordRepository.saveAll(toSave);
                }
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

        List<TopicTagResponse> topicTags = paper.getTopics().stream()
                .map(t -> TopicTagResponse.builder()
                        .topicId(t.getTopicId())
                        .topicName(t.getTopicName())
                        .build())
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
                .topics(topicTags)
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
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterKeywords(String search) {
        String searchParam = (search == null || search.trim().isEmpty()) ? "%" : "%" + search.trim() + "%";
        return keywordRepository.findTop50KeywordNamesWithCount(searchParam).stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label((String) obj[0])
                        .value((String) obj[0])
                        .count(((Number) obj[1]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterJournals(String search) {
        String searchParam = (search == null || search.trim().isEmpty()) ? "%" : "%" + search.trim() + "%";
        return journalRepository.findTop50JournalNamesWithCount(searchParam).stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label((String) obj[0])
                        .value((String) obj[0])
                        .count(((Number) obj[1]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterYears(String search) {
        String searchParam = (search == null || search.trim().isEmpty()) ? "%" : "%" + search.trim() + "%";
        return paperRepository.findDistinctYearsWithCount(searchParam).stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label(String.valueOf(obj[0]))
                        .value(String.valueOf(obj[0]))
                        .count(((Number) obj[1]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse> getFilterTopics(String search) {
        String searchParam = (search == null || search.trim().isEmpty()) ? "%" : "%" + search.trim() + "%";
        return topicRepository.findTop50TopicsWithCount(searchParam).stream()
                .map(obj -> com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse.builder()
                        .label((String) obj[1])
                        .value(String.valueOf(obj[0]))
                        .count(((Number) obj[2]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}
