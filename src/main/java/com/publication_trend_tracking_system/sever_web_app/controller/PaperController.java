package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.service.PaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/papers")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @PostMapping
    public ApiResponse<PaperResponse> createPaper(@Valid @RequestBody PaperRequest request) {
        return ApiResponse.<PaperResponse>builder()
                .code(1000)
                .message("Create paper success")
                .result(paperService.createPaper(request))
                .build();
    }

    @GetMapping
    public ApiResponse<Page<PaperResponse>> searchPapers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String journal,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) java.util.List<String> types,
            @RequestParam(required = false) Integer fieldId,
            @RequestParam(required = false) Integer topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publicationYear") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        // Whitelist allowed sort fields to prevent injection
        String safeSortBy = switch (sortBy) {
            case "publicationYear", "title", "createdAt" -> sortBy;
            default -> "publicationYear";
        };

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(safeSortBy).ascending()
                : Sort.by(safeSortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ApiResponse.<Page<PaperResponse>>builder()
                .code(1000)
                .message("Get papers success")
                .result(paperService.searchPapers(keyword, author, journal, fromYear, toYear, institution, types, fieldId, topicId, pageable))
                .build();
    }

    @GetMapping("/{paperId}")
    public ApiResponse<PaperResponse> getPaperById(@PathVariable Long paperId) {
        return ApiResponse.<PaperResponse>builder()
                .code(1000)
                .message("Get paper success")
                .result(paperService.getPaperById(paperId))
                .build();
    }

    @PutMapping("/{paperId}")
    public ApiResponse<PaperResponse> updatePaper(
            @PathVariable Long paperId,
            @Valid @RequestBody PaperRequest request) {

        return ApiResponse.<PaperResponse>builder()
                .code(1000)
                .message("Update paper success")
                .result(paperService.updatePaper(paperId, request))
                .build();
    }

    @DeleteMapping("/{paperId}")
    public ApiResponse<Void> deletePaper(@PathVariable Long paperId) {
        paperService.deletePaper(paperId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Delete paper success")
                .build();
    }

    @GetMapping("/filters/keywords")
    public ApiResponse<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>> getFilterKeywords() {
        return ApiResponse.<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>>builder()
                .code(1000)
                .message("Get filter keywords success")
                .result(paperService.getFilterKeywords())
                .build();
    }

    @GetMapping("/filters/journals")
    public ApiResponse<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>> getFilterJournals() {
        return ApiResponse.<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>>builder()
                .code(1000)
                .message("Get filter journals success")
                .result(paperService.getFilterJournals())
                .build();
    }

    @GetMapping("/filters/years")
    public ApiResponse<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>> getFilterYears() {
        return ApiResponse.<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>>builder()
                .code(1000)
                .message("Get filter years success")
                .result(paperService.getFilterYears())
                .build();
    }

    @GetMapping("/filters/topics")
    public ApiResponse<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>> getFilterTopics() {
        return ApiResponse.<java.util.List<com.publication_trend_tracking_system.sever_web_app.dto.response.FilterSuggestionResponse>>builder()
                .code(1000)
                .message("Get filter topics success")
                .result(paperService.getFilterTopics())
                .build();
    }
}
