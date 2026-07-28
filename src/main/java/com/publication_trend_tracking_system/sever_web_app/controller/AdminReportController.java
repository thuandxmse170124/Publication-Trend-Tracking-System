package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdateReportRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@SecurityRequirement(name = "api")

@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ApiResponse<?> getAllReports() {

        return ApiResponse.builder()
                .code(1000)
                .message("Success")
                .result(
                        reportService.getAllReports())
                .build();
    }

    @PatchMapping("/{reportId}")
    public ApiResponse<?> updateReport(
            @PathVariable Long reportId,
            @RequestBody UpdateReportRequest request) {

        reportService.updateReport(
                reportId,
                request);

        return ApiResponse.builder()
                .code(1000)
                .message("Report updated successfully")
                .build();
    }
}