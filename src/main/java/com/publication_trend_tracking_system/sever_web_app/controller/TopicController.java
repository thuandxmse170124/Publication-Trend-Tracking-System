package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicResponse;
import com.publication_trend_tracking_system.sever_web_app.service.TopicService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/topics")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public ApiResponse<Page<TopicResponse>> getAllTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "topicId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ApiResponse.<Page<TopicResponse>>builder()
                .code(1000)
                .message("Get topics success")
                .result(topicService.getAllTopics(pageable))
                .build();
    }

    @GetMapping("/trending")
    public ApiResponse<java.util.List<TopicResponse>> getTrendingTopics() {
        return ApiResponse.<java.util.List<TopicResponse>>builder()
                .code(1000)
                .message("Get trending topics success")
                .result(topicService.getTrendingTopics())
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
