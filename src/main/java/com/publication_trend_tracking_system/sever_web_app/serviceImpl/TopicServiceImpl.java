package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.Keyword;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicRepository;
import com.publication_trend_tracking_system.sever_web_app.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final PaperRepository paperRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TopicResponse> getAllTopics(Pageable pageable) {
        Page<Topic> topics = topicRepository.findAll(pageable);
        return topics.map(topic -> {
            long paperCount = topicRepository.countPapersByTopicId(topic.getTopicId());
            return TopicResponse.builder()
                    .topicId(topic.getTopicId())
                    .topicName(topic.getTopicName())
                    .description(topic.getDescription())
                    .paperCount(paperCount)
                    .latestPapers(null)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TopicResponse getTopicById(Integer topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));

        long paperCount = topicRepository.countPapersByTopicId(topicId);
        List<Paper> latestPapers = paperRepository.findTop10ByTopics_TopicIdOrderByCreatedAtDesc(topicId);

        List<PaperResponse> paperResponses = latestPapers.stream()
                .map(this::toPaperResponse)
                .toList();

        return TopicResponse.builder()
                .topicId(topic.getTopicId())
                .topicName(topic.getTopicName())
                .description(topic.getDescription())
                .paperCount(paperCount)
                .latestPapers(paperResponses)
                .build();
    }

    private PaperResponse toPaperResponse(Paper paper) {
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
                .createdAt(paper.getCreatedAt())
                .updatedAt(paper.getUpdatedAt())
                .authors(authorResponses)
                .keywords(keywordStrings)
                .topics(topicStrings)
                .build();
    }
}
