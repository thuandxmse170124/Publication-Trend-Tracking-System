package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SearchHistoryResponse;
import com.publication_trend_tracking_system.sever_web_app.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/search-history")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping
    public ApiResponse<Void> saveSearchHistory(
            @RequestParam String keyword,
            Authentication authentication
    ) {
        searchHistoryService.saveSearchHistory(keyword, authentication.getName());
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Save search history successfully")
                .build();
    }

    @GetMapping
    public ApiResponse<List<SearchHistoryResponse>> getRecentHistory(
            Authentication authentication
    ) {
        return ApiResponse.<List<SearchHistoryResponse>>builder()
                .code(1000)
                .message("Get recent search history successfully")
                .result(searchHistoryService.getRecentSearchHistory(authentication.getName()))
                .build();
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<PaperResponse>> getSuggestions(
            Authentication authentication
    ) {
        return ApiResponse.<List<PaperResponse>>builder()
                .code(1000)
                .message("Get paper suggestions based on history successfully")
                .result(searchHistoryService.getSuggestionsBasedOnHistory(authentication.getName()))
                .build();
    }
    
    @DeleteMapping("/{historyId}")
    public ApiResponse<Void> deleteSearchHistory(
            @PathVariable Long historyId,
            Authentication authentication
    ) {
        searchHistoryService.deleteSearchHistory(historyId, authentication.getName());
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Delete search history successfully")
                .build();
    }
}
