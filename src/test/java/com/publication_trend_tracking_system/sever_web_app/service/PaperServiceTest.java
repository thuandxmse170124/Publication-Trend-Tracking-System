package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.*;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperPublicationType;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.repository.*;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.PaperServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaperServiceTest {

    @Mock
    private PaperRepository paperRepository;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private ResearchFieldRepository researchFieldRepository;
    @Mock
    private ApiSourceRepository apiSourceRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private PaperServiceImpl paperService;

    private Paper paper;
    private Journal journal;
    private ResearchField field;
    private ApiSource apiSource;
    private Author author;
    private Keyword keyword;
    private Topic topic;

    @BeforeEach
    void setUp() {
        journal = Journal.builder().journalId(1).name("Nature").status("active").build();
        field = ResearchField.builder().fieldId(1).fieldName("Computer Science").build();
        apiSource = ApiSource.builder().sourceId(1).sourceName("OpenAlex").status("active").build();
        author = Author.builder().authorId(1L).fullName("John Doe").build();
        keyword = Keyword.builder().keywordId(1).keywordName("deep learning").build();
        topic = Topic.builder().topicId(1).topicName("Machine Learning").build();

        paper = Paper.builder()
                .paperId(1L)
                .journal(journal)
                .field(field)
                .apiSource(apiSource)
                .publicationType(PaperPublicationType.JOURNAL_ARTICLE)
                .title("Deep learning trends")
                .paperAbstract("Abstract content")
                .publicationYear(2023)
                .doi("10.1038/nature12345")
                .sourceUrl("http://nature.com/12345")
                .citationCount(100)
                .visibilityStatus(PaperVisibilityStatus.VISIBLE)
                .authors(new HashSet<>(Collections.singletonList(author)))
                .keywords(new HashSet<>(Collections.singletonList(keyword)))
                .topics(new HashSet<>(Collections.singletonList(topic)))
                .build();
    }

    @Test
    void getPaperById_Success() {
        when(paperRepository.findById(1L)).thenReturn(Optional.of(paper));

        PaperResponse response = paperService.getPaperById(1L);

        assertNotNull(response);
        assertEquals(paper.getTitle(), response.getTitle());
        assertEquals(1, response.getAuthors().size());
        assertEquals("John Doe", response.getAuthors().get(0).getFullName());
        assertEquals(1, response.getKeywords().size());
        assertEquals("deep learning", response.getKeywords().get(0));
        assertEquals(1, response.getTopics().size());
        assertEquals("Machine Learning", response.getTopics().get(0));
    }

    @Test
    void getPaperById_NotFound() {
        when(paperRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> paperService.getPaperById(1L));
        verify(paperRepository, times(1)).findById(1L);
    }

    @Test
    void searchPapers_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Paper> page = new PageImpl<>(Collections.singletonList(paper), pageable, 1);
        when(paperRepository.searchPapers("deep", "John", "Nature", 2023, 1, null, pageable))
                .thenReturn(page);

        Page<PaperResponse> responses = paperService.searchPapers("deep", "John", "Nature", 2023, 1, null, pageable);

        assertNotNull(responses);
        assertEquals(1, responses.getTotalElements());
        assertEquals("Deep learning trends", responses.getContent().get(0).getTitle());
    }

    @Test
    void createPaper_Success() {
        PaperRequest request = PaperRequest.builder()
                .journalId(1)
                .fieldId(1)
                .apiSourceId(1)
                .publicationType(PaperPublicationType.JOURNAL_ARTICLE)
                .title("Deep learning trends")
                .paperAbstract("Abstract content")
                .publicationYear(2023)
                .doi("10.1038/nature12345")
                .sourceUrl("http://nature.com/12345")
                .citationCount(100)
                .visibilityStatus(PaperVisibilityStatus.VISIBLE)
                .authorIds(Collections.singletonList(1L))
                .keywords(Collections.singletonList("deep learning"))
                .topicIds(Collections.singletonList(1))
                .build();

        when(journalRepository.findById(1)).thenReturn(Optional.of(journal));
        when(researchFieldRepository.findById(1)).thenReturn(Optional.of(field));
        when(apiSourceRepository.findById(1)).thenReturn(Optional.of(apiSource));
        when(authorRepository.findAllById(Collections.singletonList(1L))).thenReturn(Collections.singletonList(author));
        when(keywordRepository.findByKeywordNameIgnoreCase("deep learning")).thenReturn(Optional.of(keyword));
        when(topicRepository.findAllById(Collections.singletonList(1))).thenReturn(Collections.singletonList(topic));
        when(paperRepository.existsByDoi(anyString())).thenReturn(false);
        when(paperRepository.save(any(Paper.class))).thenReturn(paper);

        PaperResponse response = paperService.createPaper(request);

        assertNotNull(response);
        assertEquals(request.getTitle(), response.getTitle());
    }
}
