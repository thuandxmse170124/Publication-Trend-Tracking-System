package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicResponse;

import java.util.List;

public interface TopicService {
    List<TopicResponse> getAllTopics();
    TopicResponse getTopicById(Integer topicId);
}
