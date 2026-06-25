package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicResponse;
import com.publication_trend_tracking_system.sever_web_app.service.TopicService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/topics")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public ApiResponse<List<TopicResponse>> getAllTopics() {
        return ApiResponse.<List<TopicResponse>>builder()
                .code(1000)
                .message("Get topics success")
                .result(topicService.getAllTopics())
                .build();
    }

    @GetMapping("/{topicId}")
    public ApiResponse<TopicResponse> getTopicById(@PathVariable Integer topicId) {
        return ApiResponse.<TopicResponse>builder()
                .code(1000)
                .message("Get topic detail success")
                .result(topicService.getTopicById(topicId))
                .build();
    }
}
