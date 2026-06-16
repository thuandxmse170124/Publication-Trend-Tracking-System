package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowJournalRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowTopicRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.service.FollowService;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowAuthorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/topics")
    public ApiResponse<?> followTopic(
            @RequestBody FollowTopicRequest request,
            Authentication authentication) {

        followService.followTopic(
                request,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Topic followed successfully")
                .build();
    }

    @GetMapping("/topics")
    public ApiResponse<?> getMyFollowedTopics(
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        followService.getMyFollowedTopics(
                                authentication.getName()))
                .build();
    }

    @DeleteMapping("/topics")
    public ApiResponse<?> unfollowTopic(
            @RequestParam String topicId,
            Authentication authentication) {

        followService.unfollowTopic(
                topicId,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Topic unfollowed successfully")
                .build();
    }
    @PostMapping("/journals")
    public ApiResponse<?> followJournal(
            @RequestBody FollowJournalRequest request,
            Authentication authentication) {

        followService.followJournal(
                request,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Journal followed successfully")
                .build();
    }
    @GetMapping("/journals")
    public ApiResponse<?> getMyFollowedJournals(
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        followService
                                .getMyFollowedJournals(
                                        authentication.getName()))
                .build();
    }
    @DeleteMapping("/journals")
    public ApiResponse<?> unfollowJournal(
            @RequestParam String journalId,
            Authentication authentication) {

        followService.unfollowJournal(
                journalId,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Journal unfollowed successfully")
                .build();
    }
    @PostMapping("/authors")
    public ApiResponse<?> followAuthor(
            @RequestBody FollowAuthorRequest request,
            Authentication authentication) {

        followService.followAuthor(
                request,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Author followed successfully")
                .build();
    }
    @GetMapping("/authors")
    public ApiResponse<?> getMyFollowedAuthors(
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        followService
                                .getMyFollowedAuthors(
                                        authentication.getName()))
                .build();
    }
    @DeleteMapping("/authors")
    public ApiResponse<?> unfollowAuthor(
            @RequestParam String authorId,
            Authentication authentication) {

        followService.unfollowAuthor(
                authorId,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message("Author unfollowed successfully")
                .build();
    }
}