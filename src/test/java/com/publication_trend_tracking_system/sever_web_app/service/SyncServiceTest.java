package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.entity.ApiSource;
import com.publication_trend_tracking_system.sever_web_app.entity.Author;
import com.publication_trend_tracking_system.sever_web_app.entity.Journal;
import com.publication_trend_tracking_system.sever_web_app.entity.Keyword;
import com.publication_trend_tracking_system.sever_web_app.entity.ResearchField;
import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicDomain;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicField;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicSubfield;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        // IDs must be set: the production code maps Optional<Entity> -> Optional<Integer> via
        // getXxxId(), and an entity with a null ID falls through to the (unstubbed) save() path.
        Keyword mockKeyword = new Keyword();
        mockKeyword.setKeywordId(1);
        when(keywordRepository.findFirstByKeywordNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(mockKeyword));

        Topic mockTopic = new Topic();
        mockTopic.setTopicId(1);
        when(topicRepository.findFirstByTopicNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(mockTopic));

        ResearchField mockField = new ResearchField();
        mockField.setFieldId(1);
        when(researchFieldRepository.findFirstByFieldNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(mockField));

        // Journal and Author are expected to go through the create path (verified below via
        // save()), so unlike the lookups above, stub save() itself to return an ID-bearing entity.
        Journal mockJournal = new Journal();
        mockJournal.setJournalId(1);
        when(journalRepository.save(any())).thenReturn(mockJournal);

        Author mockAuthor = new Author();
        mockAuthor.setAuthorId(1L);
        when(authorRepository.save(any())).thenReturn(mockAuthor);

        int[] counts = new int[2];

        // Act
        syncService.saveResultsInTransaction(mockOpenAlexJson, source, "Machine Learning", counts, new java.util.ArrayList<>(), null, new java.util.HashSet<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                1L);

        // Assert
        assertEquals(1, counts[0]);
        verify(paperRepository, times(1)).save(any());
        verify(authorRepository, times(1)).save(any());
        verify(journalRepository, times(1)).save(any());
    }

    @Test
    void testSaveResultsInTransaction_StructuredOpenAlex_UsesOfficialTopicAndFieldHierarchy() {
        // Arrange: a response using the new "topics" taxonomy array (not "concepts"), matched
        // by OpenAlex ID rather than free-text search.
        String mockJson = """
                {
                  "results": [
                    {
                      "id": "https://openalex.org/W999",
                      "doi": "https://doi.org/10.999/xyz",
                      "title": "Ferroelectric Thin Films",
                      "publication_year": 2024,
                      "cited_by_count": 3,
                      "primary_location": { "source": { "display_name": "Applied Physics Letters" } },
                      "authorships": [ { "author": { "display_name": "Jane Roe" } } ],
                      "topics": [
                        { "id": "https://openalex.org/T10068", "display_name": "Ferroelectric and Piezoelectric Materials" }
                      ]
                    }
                  ]
                }
                """;

        ApiSource source = new ApiSource();
        source.setSourceName("OpenAlex");

        TopicDomain domain = TopicDomain.builder().domainId(1).openalexId("3").displayName("Physical Sciences").build();
        TopicField field = TopicField.builder().fieldId(1).openalexId("25").displayName("Materials Science").domain(domain).build();
        TopicSubfield subfield = TopicSubfield.builder().subfieldId(1).openalexId("2504").displayName("Electronic Materials").field(field).build();
        Topic officialTopic = Topic.builder().topicId(7).openalexId("T10068").topicName("Ferroelectric and Piezoelectric Materials").subfield(subfield).build();

        when(topicRepository.findById(7)).thenReturn(Optional.of(officialTopic));
        when(topicRepository.findByOpenalexId("T10068")).thenReturn(Optional.of(officialTopic));

        ResearchField mockField = new ResearchField();
        mockField.setFieldId(9);
        when(researchFieldRepository.findFirstByFieldNameIgnoreCase("Materials Science"))
                .thenReturn(Optional.of(mockField));

        Author mockAuthor = new Author();
        mockAuthor.setAuthorId(2L);
        when(authorRepository.save(any())).thenReturn(mockAuthor);

        Journal mockJournal = new Journal();
        mockJournal.setJournalId(2);
        when(journalRepository.save(any())).thenReturn(mockJournal);

        int[] counts = new int[2];

        // Act
        boolean continuePagination = syncService.saveResultsInTransaction(
                mockJson, source, 7, counts, new java.util.ArrayList<>(), null, new java.util.HashSet<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                1L);

        // Assert
        assertTrue(continuePagination);
        assertEquals(1, counts[0]);
        // ResearchField is derived from the topic's official Field tier, not an ad-hoc name
        verify(researchFieldRepository, times(1)).findFirstByFieldNameIgnoreCase("Materials Science");
        // Topic assignment resolved by canonical OpenAlex ID, not by name matching
        verify(topicRepository, times(1)).findByOpenalexId("T10068");
        verify(paperRepository, times(1)).save(any());
    }

    @Test
    void testSaveResultsInTransaction_StructuredOpenAlex_TopicVanishedMidRun_SkipsGracefully() {
        // If the topic row disappears between being listed and being processed (edge case),
        // the job should skip it without throwing and without touching paperRepository.
        when(topicRepository.findById(999)).thenReturn(Optional.empty());

        int[] counts = new int[2];

        boolean continuePagination = syncService.saveResultsInTransaction(
                "{}", new ApiSource(), 999, counts, new java.util.ArrayList<>(), null, new java.util.HashSet<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                1L);

        assertTrue(continuePagination);
        verifyNoInteractions(paperRepository);
    }
}
