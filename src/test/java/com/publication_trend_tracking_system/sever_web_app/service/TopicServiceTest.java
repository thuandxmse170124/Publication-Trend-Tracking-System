package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperPublicationType;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.TopicServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private PaperRepository paperRepository;

    @InjectMocks
    private TopicServiceImpl topicService;

    private Topic topic;
    private Paper paper;

    @BeforeEach
    void setUp() {
        topic = Topic.builder()
                .topicId(1)
                .topicName("Machine Learning")
                .description("Research related to machine learning models")
                .build();

        paper = Paper.builder()
                .paperId(1L)
                .title("A Study on ML")
                .publicationYear(2023)
                .citationCount(10)
                .publicationType(PaperPublicationType.JOURNAL_ARTICLE)
                .visibilityStatus(PaperVisibilityStatus.VISIBLE)
                .authors(new HashSet<>())
                .keywords(new HashSet<>())
                .topics(new HashSet<>(Collections.singletonList(topic)))
                .build();
    }

    @Test
    void getAllTopics_Success() {
        when(topicRepository.findAll()).thenReturn(Collections.singletonList(topic));
        when(topicRepository.countPapersByTopicId(1)).thenReturn(5L);

        List<TopicResponse> responses = topicService.getAllTopics();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(topic.getTopicName(), responses.get(0).getTopicName());
        assertEquals(5L, responses.get(0).getPaperCount());
        assertNull(responses.get(0).getLatestPapers());
        verify(topicRepository, times(1)).findAll();
        verify(topicRepository, times(1)).countPapersByTopicId(1);
    }

    @Test
    void getTopicById_Success() {
        when(topicRepository.findById(1)).thenReturn(Optional.of(topic));
        when(topicRepository.countPapersByTopicId(1)).thenReturn(5L);
        when(paperRepository.findTop10ByTopics_TopicIdOrderByCreatedAtDesc(1))
                .thenReturn(Collections.singletonList(paper));

        TopicResponse response = topicService.getTopicById(1);

        assertNotNull(response);
        assertEquals(topic.getTopicName(), response.getTopicName());
        assertEquals(5L, response.getPaperCount());
        assertNotNull(response.getLatestPapers());
        assertEquals(1, response.getLatestPapers().size());
        assertEquals(paper.getTitle(), response.getLatestPapers().get(0).getTitle());
        verify(topicRepository, times(1)).findById(1);
        verify(topicRepository, times(1)).countPapersByTopicId(1);
        verify(paperRepository, times(1)).findTop10ByTopics_TopicIdOrderByCreatedAtDesc(1);
    }

    @Test
    void getTopicById_NotFound() {
        when(topicRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> topicService.getTopicById(1));
        verify(topicRepository, times(1)).findById(1);
        verify(topicRepository, never()).countPapersByTopicId(anyInt());
        verify(paperRepository, never()).findTop10ByTopics_TopicIdOrderByCreatedAtDesc(anyInt());
    }
}
