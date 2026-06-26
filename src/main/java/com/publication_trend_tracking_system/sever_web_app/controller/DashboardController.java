package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PersonalStatsResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SystemStatsResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.YearCountResponse;
import com.publication_trend_tracking_system.sever_web_app.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/member/dashboard")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/system-stats")
    public ApiResponse<SystemStatsResponse> getSystemStats() {
        return ApiResponse.<SystemStatsResponse>builder()
                .code(1000)
                .message("Get system stats success")
                .result(dashboardService.getSystemStats())
                .build();
    }

    @GetMapping("/personal-stats")
    public ApiResponse<PersonalStatsResponse> getPersonalStats() {
        return ApiResponse.<PersonalStatsResponse>builder()
                .code(1000)
                .message("Get personal stats success")
                .result(dashboardService.getPersonalStats())
                .build();
    }

    @GetMapping("/trend/chart")
    public ApiResponse<List<YearCountResponse>> getTrendChartData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String journal,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) java.util.List<String> types,
            @RequestParam(required = false) Boolean isOpenAccess,
            @RequestParam(required = false) Integer fieldId,
            @RequestParam(required = false) Integer topicId) {

        return ApiResponse.<List<YearCountResponse>>builder()
                .code(1000)
                .message("Get trend chart data success")
                .result(dashboardService.getTrendChartData(keyword, author, journal, fromYear, toYear, institution, types, isOpenAccess, fieldId, topicId))
                .build();
    }
}
