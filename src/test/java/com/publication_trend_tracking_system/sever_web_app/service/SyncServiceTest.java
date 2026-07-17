package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.entity.ApiSource;
import com.publication_trend_tracking_system.sever_web_app.entity.Keyword;
import com.publication_trend_tracking_system.sever_web_app.entity.ResearchField;
import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.repository.*;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.SyncServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private SyncJobRepository syncJobRepository;
    @Mock
    private ApiSourceRepository apiSourceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private ResearchFieldRepository researchFieldRepository;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        // Reflection to inject EntityManager since it's using @PersistenceContext
        try {
            java.lang.reflect.Field emField = SyncServiceImpl.class.getDeclaredField("entityManager");
            emField.setAccessible(true);
            emField.set(syncService, entityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSaveResultsInTransaction_OpenAlex_ParsingLogic() {
        // Arrange
        String mockOpenAlexJson = """
                {
                  "results": [
                    {
                      "id": "https://openalex.org/W123",
                      "doi": "https://doi.org/10.123/456",
                      "title": "Machine Learning in Medicine",
                      "publication_year": 2023,
                      "cited_by_count": 42,
                      "primary_location": {
                        "source": { "display_name": "Nature Medicine" }
                      },
                      "authorships": [
                        { "author": { "display_name": "John Doe" } }
                      ],
                      "concepts": [
                        { "level": 1, "display_name": "Computer Science" }
                      ],
                      "abstract_inverted_index": {
                        "This": [0], "is": [1], "abstract": [2]
                      }
                    }
                  ]
                }
                """;

        ApiSource source = new ApiSource();
        source.setSourceName("OpenAlex");

        when(keywordRepository.findFirstByKeywordNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(new Keyword()));
        when(topicRepository.findFirstByTopicNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(new Topic()));
        when(researchFieldRepository.findFirstByFieldNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(new ResearchField()));

        int[] counts = new int[2];

        // Act
        syncService.saveResultsInTransaction(mockOpenAlexJson, source, "Machine Learning", counts, new java.util.ArrayList<>(), null);

        // Assert
        assertEquals(1, counts[0]);
        verify(paperRepository, times(1)).save(any());
        verify(authorRepository, times(1)).save(any());
        verify(journalRepository, times(1)).save(any());
    }
}
