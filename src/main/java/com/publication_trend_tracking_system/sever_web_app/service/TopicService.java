package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TopicService {
    Page<TopicResponse> getAllTopics(Pageable pageable);
    TopicResponse getTopicById(Integer topicId);
}

