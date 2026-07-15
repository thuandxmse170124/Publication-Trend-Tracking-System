package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.*;
import com.publication_trend_tracking_system.sever_web_app.service.CitationGraphService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                .result(citationGraphService.getCitationGraph(paperId))
                .build();
    }

    @GetMapping("/openalex/{openAlexId}/preview")
    public ApiResponse<CitationPaperPreviewResponse> getPaperPreview(
            @PathVariable String openAlexId
    ) {

        return ApiResponse.<CitationPaperPreviewResponse>builder()
                .code(1000)
                .message("Get Paper Preview Success")
                .result(citationGraphService.getPaperPreview(openAlexId))
                .build();
    }

    @GetMapping("/authors/openalex/{authorId}/preview")
    public ApiResponse<CitationAuthorPreviewResponse> getAuthorPreview(
            @PathVariable String authorId
    ) {

        return ApiResponse.<CitationAuthorPreviewResponse>builder()
                .code(1000)
                .message("Get Author Preview Success")
                .result(citationGraphService.getAuthorPreview(authorId))
                .build();
    }

    @GetMapping("/{paperId}/references")
    public ApiResponse<List<CitationPaperNodeResponse>> getReferences(
            @PathVariable Long paperId
    ) {

        return ApiResponse.<List<CitationPaperNodeResponse>>builder()
                .code(1000)
                .message("Get References Success")
                .result(
                        citationGraphService.getReferences(paperId)
                )
                .build();
    }

    @GetMapping("/{paperId}/cited-by")
    public ApiResponse<List<CitationPaperNodeResponse>> getCitedBy(
            @PathVariable Long paperId
    ) {

        return ApiResponse.<List<CitationPaperNodeResponse>>builder()
                .code(1000)
                .message("Get Cited By Success")
                .result(
                        citationGraphService.getCitedBy(paperId)
                )
                .build();
    }
    @GetMapping("/{paperId}/relationship/{referenceOpenAlexId}")
    public ApiResponse<CitationRelationshipResponse> getRelationship(
            @PathVariable Long paperId,
            @PathVariable String referenceOpenAlexId
    ) {

        return ApiResponse
                .<CitationRelationshipResponse>builder()
                .code(1000)
                .message("Get Relationship Analysis Success")
                .result(
                        citationGraphService.getRelationship(
                                paperId,
                                referenceOpenAlexId
                        )
                )
                .build();
    }

    /** A navigation-first view: what this paper means and what to read next. */
    @GetMapping("/{paperId}/research-guide")
    public ApiResponse<ResearchGuideResponse> getResearchGuide(
            @PathVariable Long paperId
    ) {
        return ApiResponse.<ResearchGuideResponse>builder()
                .code(1000)
                .message("Get Research Guide Success")
                .result(citationGraphService.getResearchGuide(paperId))
                .build();
    }

}
