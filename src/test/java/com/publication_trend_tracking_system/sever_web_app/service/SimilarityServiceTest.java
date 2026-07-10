package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.serviceImpl.SimilarityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SimilarityServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @InjectMocks
    private SimilarityServiceImpl similarityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testServiceInstantiationAndMocking() {
        Paper paper1 = new Paper();
        paper1.setPaperId(1L);
        paper1.setTitle("Machine Learning and Artificial Intelligence");
        // Assuming paper doesn't have an abstract field accessible like this, we skip it
        paper1.setCitationCount(50);
        paper1.setPublicationYear(2023);

        when(paperRepository.findAll()).thenReturn(Arrays.asList(paper1));

        assertNotNull(similarityService, "Similarity Service should be successfully injected");
    }
}
