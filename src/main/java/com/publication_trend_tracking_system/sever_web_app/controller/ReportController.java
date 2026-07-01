package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateReportRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService
            reportService;

    @PostMapping
    public ApiResponse<?> createReport(
            @RequestBody CreateReportRequest request,
            Authentication authentication) {

        reportService.createReport(
                request,
                authentication.getName());

        return ApiResponse.builder()
                .code(1000)
                .message(
                        "Report created successfully")
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<?> getMyReports(
            Authentication authentication) {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        reportService.getMyReports(
                                authentication.getName()))
                .build();
    }

    @GetMapping("/admin")
    public ApiResponse<?> getAllReports() {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        reportService.getAllReports())
                .build();
    }
}