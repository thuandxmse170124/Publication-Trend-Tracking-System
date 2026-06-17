package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.ApiSource;
import com.publication_trend_tracking_system.sever_web_app.entity.SyncJob;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.ApiSourceRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.SyncJobRepository;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.SyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SyncServiceTest {

    @Mock
    private SyncJobRepository syncJobRepository;

    @Mock
    private ApiSourceRepository apiSourceRepository;

    @Mock
    private com.publication_trend_tracking_system.sever_web_app.repository.UserRepository userRepository;

    @Mock
    private com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository paperRepository;

    @Mock
    private com.publication_trend_tracking_system.sever_web_app.repository.JournalRepository journalRepository;

    @Mock
    private com.publication_trend_tracking_system.sever_web_app.repository.AuthorRepository authorRepository;

    @Mock
    private com.publication_trend_tracking_system.sever_web_app.repository.KeywordRepository keywordRepository;

    @Mock
    private com.publication_trend_tracking_system.sever_web_app.repository.TopicRepository topicRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SyncServiceImpl syncService;

    private ApiSource activeSource;
    private ApiSource inactiveSource;

    @BeforeEach
    void setUp() {
        syncService.restTemplate = restTemplate;

        activeSource = ApiSource.builder()
                .sourceId(1)
                .sourceName("OpenAlex")
                .baseUrl("https://api.openalex.org")
                .status("ACTIVE")
                .build();

        inactiveSource = ApiSource.builder()
                .sourceId(2)
                .sourceName("Semantic Scholar")
                .baseUrl("https://api.semanticscholar.org/graph")
                .status("INACTIVE")
                .build();
    }

    @Test
    void syncFromSource_ApiSourceNotFound() {
        when(apiSourceRepository.findById(999)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> syncService.syncFromSource(999, null, "test"));
        assertEquals(ErrorCode.API_SOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void syncFromSource_ApiSourceInactive() {
        when(apiSourceRepository.findById(2)).thenReturn(Optional.of(inactiveSource));

        AppException ex = assertThrows(AppException.class, () -> syncService.syncFromSource(2, null, "test"));
        assertEquals(ErrorCode.API_SOURCE_INACTIVE, ex.getErrorCode());
    }

    @Test
    void syncFromSource_Success() {
        when(apiSourceRepository.findById(1)).thenReturn(Optional.of(activeSource));

        // Mock saving SyncJob initially
        SyncJob initialJob = SyncJob.builder()
                .syncJobId(100L)
                .apiSource(activeSource)
                .status("RUNNING")
                .build();
        when(syncJobRepository.save(any(SyncJob.class))).thenReturn(initialJob);

        com.publication_trend_tracking_system.sever_web_app.entity.Keyword keyword = 
                com.publication_trend_tracking_system.sever_web_app.entity.Keyword.builder()
                .keywordId(10)
                .keywordName("AI")
                .build();
        when(keywordRepository.findByKeywordNameIgnoreCase("AI")).thenReturn(Optional.empty());
        when(keywordRepository.save(any(com.publication_trend_tracking_system.sever_web_app.entity.Keyword.class))).thenReturn(keyword);
        when(topicRepository.findByTopicNameIgnoreCase("AI")).thenReturn(Optional.empty());

        // Mock restTemplate call
        String sampleJson = "{\"results\":[]}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(sampleJson, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        SyncJobResponse response = syncService.syncFromSource(1, null, "AI");

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(0, response.getAddedCount());
        assertEquals(0, response.getUpdatedCount());
    }
}
