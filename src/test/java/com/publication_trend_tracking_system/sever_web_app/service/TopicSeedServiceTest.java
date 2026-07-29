package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicDomain;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicField;
import com.publication_trend_tracking_system.sever_web_app.entity.TopicSubfield;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicDomainRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicFieldRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicSubfieldRepository;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.TopicSeedServiceImpl;
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
class TopicSeedServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private TopicSubfieldRepository topicSubfieldRepository;
    @Mock
    private TopicFieldRepository topicFieldRepository;
    @Mock
    private TopicDomainRepository topicDomainRepository;
    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private TopicSeedServiceImpl topicSeedService;

    private static final String ONE_TOPIC_JSON = """
            {
              "results": [
                {
                  "id": "https://openalex.org/T10068",
                  "display_name": "Ferroelectric and Piezoelectric Materials",
                  "description": "Study of ferroelectric materials",
                  "domain": { "id": "https://openalex.org/domains/3", "display_name": "Physical Sciences" },
                  "field": { "id": "https://openalex.org/fields/25", "display_name": "Materials Science" },
                  "subfield": { "id": "https://openalex.org/subfields/2504", "display_name": "Electronic, Optical and Magnetic Materials" }
                }
              ]
            }
            """;

    @Test
    void saveTopicsPage_newTopic_createsFullHierarchyAndTopic() {
        when(topicDomainRepository.findByOpenalexId("3")).thenReturn(Optional.empty());
        when(topicDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicFieldRepository.findByOpenalexId("25")).thenReturn(Optional.empty());
        when(topicFieldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicSubfieldRepository.findByOpenalexId("2504")).thenReturn(Optional.empty());
        when(topicSubfieldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.findByOpenalexId("T10068")).thenReturn(Optional.empty());
        when(topicRepository.findByTopicNameIgnoreCaseAndOpenalexIdIsNull(anyString())).thenReturn(Optional.empty());

        int[] saved = topicSeedService.saveTopicsPage(ONE_TOPIC_JSON);

        assertEquals(1, saved[0]);
        assertEquals(1, saved[1]);
        verify(topicDomainRepository, times(1)).save(any());
        verify(topicFieldRepository, times(1)).save(any());
        verify(topicSubfieldRepository, times(1)).save(any());
        verify(topicRepository, times(1)).save(argThat(t ->
                "T10068".equals(t.getOpenalexId())
                        && "Ferroelectric and Piezoelectric Materials".equals(t.getTopicName())));
    }

    @Test
    void saveTopicsPage_reRunSameData_isIdempotent_noDuplicateHierarchyRows() {
        TopicDomain existingDomain = TopicDomain.builder().domainId(1).openalexId("3").displayName("Physical Sciences").build();
        TopicField existingField = TopicField.builder().fieldId(1).openalexId("25").displayName("Materials Science").domain(existingDomain).build();
        TopicSubfield existingSubfield = TopicSubfield.builder().subfieldId(1).openalexId("2504").displayName("Electronic, Optical and Magnetic Materials").field(existingField).build();
        Topic existingTopic = Topic.builder().topicId(1).openalexId("T10068").topicName("Ferroelectric and Piezoelectric Materials").subfield(existingSubfield).build();

        when(topicDomainRepository.findByOpenalexId("3")).thenReturn(Optional.of(existingDomain));
        when(topicFieldRepository.findByOpenalexId("25")).thenReturn(Optional.of(existingField));
        when(topicSubfieldRepository.findByOpenalexId("2504")).thenReturn(Optional.of(existingSubfield));
        when(topicRepository.findByOpenalexId("T10068")).thenReturn(Optional.of(existingTopic));

        int[] saved = topicSeedService.saveTopicsPage(ONE_TOPIC_JSON);

        assertEquals(1, saved[0]);
        assertEquals(1, saved[1]);
        verify(topicDomainRepository, never()).save(any());
        verify(topicFieldRepository, never()).save(any());
        verify(topicSubfieldRepository, never()).save(any());
        verify(topicRepository, times(1)).save(existingTopic);
    }

    @Test
    void saveTopicsPage_legacyTopicWithSameName_upgradesInPlaceInsteadOfDuplicating() {
        // Simulates a topic created ad-hoc from OpenAlex concepts before the official taxonomy
        // was seeded: same display name, but openalexId is still null. Must be reused in place,
        // not duplicated, since topic_name has a DB-level unique constraint.
        Topic legacyTopic = Topic.builder().topicId(42).topicName("Ferroelectric and Piezoelectric Materials").openalexId(null).build();

        when(topicDomainRepository.findByOpenalexId(anyString())).thenReturn(Optional.empty());
        when(topicDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicFieldRepository.findByOpenalexId(anyString())).thenReturn(Optional.empty());
        when(topicFieldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicSubfieldRepository.findByOpenalexId(anyString())).thenReturn(Optional.empty());
        when(topicSubfieldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.findByOpenalexId("T10068")).thenReturn(Optional.empty());
        when(topicRepository.findByTopicNameIgnoreCaseAndOpenalexIdIsNull("Ferroelectric and Piezoelectric Materials"))
                .thenReturn(Optional.of(legacyTopic));

        topicSeedService.saveTopicsPage(ONE_TOPIC_JSON);

        verify(topicRepository, times(1)).save(argThat(t ->
                t.getTopicId() != null && t.getTopicId() == 42 && "T10068".equals(t.getOpenalexId())));
    }

    @Test
    void saveTopicsPage_nameCollidesWithDifferentAlreadySeededOfficialTopic_createsNewRowInstead() {
        // Two distinct real OpenAlex topics can have names that differ only by case (e.g. T11690
        // "Advanced battery technologies research" vs T10663 "Advanced Battery Technologies
        // Research" — this exact collision was found missing from the seeded taxonomy). Once a
        // topic already has an openalexId, a different topic's name match must never steal its
        // row: findByTopicNameIgnoreCaseAndOpenalexIdIsNull must NOT match it, so a new row is
        // created instead of overwriting the existing official topic's ID.
        when(topicDomainRepository.findByOpenalexId(anyString())).thenReturn(Optional.empty());
        when(topicDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicFieldRepository.findByOpenalexId(anyString())).thenReturn(Optional.empty());
        when(topicFieldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicSubfieldRepository.findByOpenalexId(anyString())).thenReturn(Optional.empty());
        when(topicSubfieldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.findByOpenalexId("T10068")).thenReturn(Optional.empty());
        // No unlinked row shares this name — the only same-named row already has a different
        // openalexId, so the (openalexId IS NULL) filter correctly excludes it.
        when(topicRepository.findByTopicNameIgnoreCaseAndOpenalexIdIsNull("Ferroelectric and Piezoelectric Materials"))
                .thenReturn(Optional.empty());

        topicSeedService.saveTopicsPage(ONE_TOPIC_JSON);

        verify(topicRepository, times(1)).save(argThat(t ->
                t.getTopicId() == null && "T10068".equals(t.getOpenalexId())));
    }
}
