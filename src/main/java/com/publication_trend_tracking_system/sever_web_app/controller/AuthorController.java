package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.service.AuthorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/authors")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping("/{authorId}")
    public ApiResponse<AuthorResponse> getAuthorById(@PathVariable Long authorId) {
        return ApiResponse.<AuthorResponse>builder()
                .code(1000)
                .message("Get author success")
                .result(authorService.getAuthorById(authorId))
                .build();
    }
}
