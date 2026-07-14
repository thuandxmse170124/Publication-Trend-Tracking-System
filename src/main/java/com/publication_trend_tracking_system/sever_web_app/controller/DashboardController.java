package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.*;

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
    @GetMapping("/personalized")
    public ApiResponse<PersonalizedDashboardResponse>
    getPersonalizedDashboard() {

        return ApiResponse
                .<PersonalizedDashboardResponse>builder()
                .code(1000)
                .message("Success")
                .result(
                        dashboardService
                                .getPersonalizedDashboard())
                .build();
    }

    @GetMapping("/topic-trends")
    public ApiResponse<List<TopicTrendResponse>>
    getTopicTrends() {

        return ApiResponse
                .<List<TopicTrendResponse>>builder()
                .code(1000)
                .message("Success")
                .result(
                        dashboardService
                                .getTopicTrends())
                .build();
    }
}
