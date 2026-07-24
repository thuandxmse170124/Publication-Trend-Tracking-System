package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.SyncService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sync")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class AdminSyncController {

    private final SyncService syncService;
    private final UserRepository userRepository;
    private final com.publication_trend_tracking_system.sever_web_app.service.TopicSeedService topicSeedService;

    @PostMapping("/trigger/{sourceId}")
    public ApiResponse<SyncJobResponse> triggerSync(
            @PathVariable Integer sourceId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "ALL") com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {

        User user = null;
        if (authentication != null) {
            String email = authentication.getName();
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        Long userId = user != null ? user.getUserId() : null;
        SyncJobResponse result = syncService.syncFromSource(sourceId, userId, query, timeRange, fromDate, toDate);

        return ApiResponse.<SyncJobResponse>builder()
                .code(1000)
                .message("Sync job triggered successfully")
                .result(result)
                .build();
    }

    @GetMapping("/logs")
    public ApiResponse<Page<SyncJobResponse>> getSyncLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<SyncJobResponse> result = syncService.getSyncLogs(page, size);

        return ApiResponse.<Page<SyncJobResponse>>builder()
                .code(1000)
                .message("Get sync logs success")
                .result(result)
                .build();
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ApiResponse<SyncJobResponse> retrySyncJob(
            @PathVariable Long jobId,
            Authentication authentication) {

        User user = null;
        if (authentication != null) {
            String email = authentication.getName();
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        Long userId = user != null ? user.getUserId() : null;
        SyncJobResponse result = syncService.retrySyncJob(jobId, userId);

        return ApiResponse.<SyncJobResponse>builder()
                .code(1000)
                .message("Sync job retried successfully")
                .result(result)
                .build();
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ApiResponse<SyncJobResponse> cancelSyncJob(
            @PathVariable Long jobId) {

        SyncJobResponse result = syncService.cancelSyncJob(jobId);

        return ApiResponse.<SyncJobResponse>builder()
                .code(1000)
                .message("Sync job cancelled successfully")
                .result(result)
                .build();
    }

    @PostMapping("/trigger-all/{sourceId}")
    public ApiResponse<SyncJobResponse> triggerSyncAll(
            @PathVariable Integer sourceId,
            @RequestParam(defaultValue = "ALL") com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {

        User user = null;
        if (authentication != null) {
            user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        Long userId = user != null ? user.getUserId() : null;
        SyncJobResponse result = syncService.syncAll(sourceId, userId, timeRange, fromDate, toDate);

        return ApiResponse.<SyncJobResponse>builder()
                .code(1000)
                .message("Sync-all job started in background")
                .result(result)
                .build();
    }

    @GetMapping("/scheduler/status")
    public ApiResponse<Boolean> getSchedulerStatus() {
        return ApiResponse.<Boolean>builder()
                .code(1000)
                .message("Get scheduler status success")
                .result(com.publication_trend_tracking_system.sever_web_app.scheduler.SyncScheduler.isSchedulerEnabled())
                .build();
    }

    @PostMapping("/scheduler/toggle")
    public ApiResponse<Boolean> toggleScheduler(
            @RequestParam boolean enabled) {
        com.publication_trend_tracking_system.sever_web_app.scheduler.SyncScheduler.setSchedulerEnabled(enabled);
        return ApiResponse.<Boolean>builder()
                .code(1000)
                .message("Scheduler status updated successfully")
                .result(enabled)
                .build();
    }

    @PostMapping("/topics/seed")
    public ApiResponse<Void> seedTopics() {
        topicSeedService.seedOfficialTaxonomy();
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Topic taxonomy seeding triggered in background")
                .build();
    }

    @GetMapping("/topics/seed/status")
    public ApiResponse<com.publication_trend_tracking_system.sever_web_app.dto.response.TopicSeedStatusResponse> getSeedStatus() {
        return ApiResponse.<com.publication_trend_tracking_system.sever_web_app.dto.response.TopicSeedStatusResponse>builder()
                .code(1000)
                .message("Get topic seed status success")
                .result(topicSeedService.getSeedStatus())
                .build();
    }
}
