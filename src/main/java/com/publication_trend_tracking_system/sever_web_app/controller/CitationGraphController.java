package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationAuthorPreviewResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationGraphResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationPaperPreviewResponse;
import com.publication_trend_tracking_system.sever_web_app.service.CitationGraphService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/papers")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")
public class CitationGraphController {

    private final CitationGraphService citationGraphService;

    @GetMapping("/{paperId}/citation-graph")
    public ApiResponse<CitationGraphResponse> getCitationGraph(
            @PathVariable Long paperId
    ) {

        return ApiResponse.<CitationGraphResponse>builder()
                .code(1000)
                .message("Get Citation Graph Success")
                .result(
                        citationGraphService
                                .getCitationGraph(paperId)
                )
                .build();
    }
    @GetMapping("/openalex/{openAlexId}/preview")
    public ApiResponse<CitationPaperPreviewResponse> getPaperPreview(
            @PathVariable String openAlexId
    ) {

        return ApiResponse
                .<CitationPaperPreviewResponse>builder()
                .code(1000)
                .message("Get Paper Preview Success")
                .result(
                        citationGraphService
                                .getPaperPreview(openAlexId)
                )
                .build();
    }
    @GetMapping("/authors/openalex/{authorId}/preview")
    public ApiResponse<CitationAuthorPreviewResponse> getAuthorPreview(
            @PathVariable String authorId
    ) {

        return ApiResponse
                .<CitationAuthorPreviewResponse>builder()
                .code(1000)
                .message("Get Author Preview Success")
                .result(
                        citationGraphService
                                .getAuthorPreview(authorId)
                )
                .build();
    }
}