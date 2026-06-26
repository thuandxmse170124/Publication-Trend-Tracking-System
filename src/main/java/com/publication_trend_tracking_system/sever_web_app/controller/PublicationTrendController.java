package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopJournalResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopKeywordResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse;
import com.publication_trend_tracking_system.sever_web_app.service.PublicationTrendService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/member/publication-trends")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class PublicationTrendController {

    private final PublicationTrendService trendService;

    @GetMapping
    public ApiResponse<List<YearCountResponse>> getTrendChartData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String journal,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) Boolean isOpenAccess,
            @RequestParam(required = false) Integer fieldId,
            @RequestParam(required = false) Integer topicId) {

        return ApiResponse.<List<YearCountResponse>>builder()
                .code(1000)
                .message("Get publication trends chart data success")
                .result(trendService.getTrendChartData(keyword, author, journal, fromYear, toYear, institution, types, isOpenAccess, fieldId, topicId))
                .build();
    }

    @GetMapping("/top-keywords")
    public ApiResponse<List<TopKeywordResponse>> getTopKeywords() {
        return ApiResponse.<List<TopKeywordResponse>>builder()
                .code(1000)
                .message("Get top keywords success")
                .result(trendService.getTopKeywords())
                .build();
    }

    @GetMapping("/top-journals")
    public ApiResponse<List<TopJournalResponse>> getTopJournals(
            @RequestParam(required = false) Integer fieldId) {
        return ApiResponse.<List<TopJournalResponse>>builder()
                .code(1000)
                .message("Get top journals success")
                .result(trendService.getTopJournals(fieldId))
                .build();
    }
}
