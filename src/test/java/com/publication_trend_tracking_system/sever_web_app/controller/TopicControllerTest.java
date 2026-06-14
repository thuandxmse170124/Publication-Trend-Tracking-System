package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicResponse;
import com.publication_trend_tracking_system.sever_web_app.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TopicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TopicService topicService;

    @InjectMocks
    private TopicController topicController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(topicController).build();
    }

    @Test
    void getAllTopics_Success() throws Exception {
        TopicResponse topicResponse = TopicResponse.builder()
                .topicId(1)
                .topicName("Machine Learning")
                .description("ML Description")
                .paperCount(5L)
                .build();

        when(topicService.getAllTopics()).thenReturn(Collections.singletonList(topicResponse));

        mockMvc.perform(get("/api/member/topics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Get topics success"))
                .andExpect(jsonPath("$.result[0].topicId").value(1))
                .andExpect(jsonPath("$.result[0].topicName").value("Machine Learning"));

        verify(topicService, times(1)).getAllTopics();
    }

    @Test
    void getTopicById_Success() throws Exception {
        TopicResponse topicResponse = TopicResponse.builder()
                .topicId(1)
                .topicName("Machine Learning")
                .description("ML Description")
                .paperCount(5L)
                .build();

        when(topicService.getTopicById(1)).thenReturn(topicResponse);

        mockMvc.perform(get("/api/member/topics/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Get topic detail success"))
                .andExpect(jsonPath("$.result.topicId").value(1))
                .andExpect(jsonPath("$.result.topicName").value("Machine Learning"));

        verify(topicService, times(1)).getTopicById(1);
    }
}
