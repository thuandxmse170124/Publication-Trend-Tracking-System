package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobPaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicSeedStatusResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.SyncService;
import com.publication_trend_tracking_system.sever_web_app.service.TopicSeedService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sync")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class AdminSyncController {

    private final SyncService syncService;
    private final TopicSeedService topicSeedService;
    private final UserRepository userRepository;

    @PostMapping("/trigger/{sourceId}")
    public ApiResponse<SyncJobResponse> triggerSync(
            @PathVariable Integer sourceId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "ALL") com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
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

    @PostMapping("/trigger-all/{sourceId}")
    public ApiResponse<SyncJobResponse> triggerSyncAll(
            @PathVariable Integer sourceId,
            @RequestParam(defaultValue = "ALL") com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
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

    @GetMapping("/jobs/{jobId}/papers")
    public ApiResponse<Page<SyncJobPaperResponse>> getSyncJobPapers(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<SyncJobPaperResponse> result = syncService.getSyncJobPapers(jobId, page, size);

        return ApiResponse.<Page<SyncJobPaperResponse>>builder()
                .code(1000)
                .message("Get sync job papers success")
                .result(result)
                .build();
    }

    @PostMapping("/jobs/{jobId}/stop")
    public ApiResponse<Void> stopSyncJob(@PathVariable Long jobId) {
        syncService.stopSyncJob(jobId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Stop signal sent to sync job")
                .build();
    }

    // Stop halts a run but keeps its record; this removes the record entirely once it is no longer
    // running. Papers already synced by the job are kept — they are real data, not job scratch.
    @DeleteMapping("/jobs/{jobId}")
    public ApiResponse<Void> deleteSyncJob(@PathVariable Long jobId) {
        syncService.deleteSyncJob(jobId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Sync job deleted")
                .build();
    }

    @GetMapping("/scheduler/status")
    public ApiResponse<Boolean> getSchedulerStatus() {
        return ApiResponse.<Boolean>builder()
                .code(1000)
                .message("Get background scheduler status success")
                .result(syncService.isSchedulerEnabled())
                .build();
    }

    @PostMapping("/scheduler/toggle")
    public ApiResponse<Boolean> toggleScheduler(@RequestParam boolean enabled) {
        boolean result = syncService.setSchedulerEnabled(enabled);
        return ApiResponse.<Boolean>builder()
                .code(1000)
                .message("Background scheduler " + (result ? "enabled" : "disabled"))
                .result(result)
                .build();
    }

    @PostMapping("/seed-topics")
    public ApiResponse<Void> seedTopics() {
        topicSeedService.seedOfficialTaxonomy();
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("OpenAlex topic taxonomy seed job started in background")
                .build();
    }

    @PostMapping("/seed-topics/cancel")
    public ApiResponse<Void> cancelSeedTopics() {
        topicSeedService.cancelSeed();
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Cancel signal sent to topic taxonomy seed")
                .build();
    }

    @GetMapping("/seed-topics/status")
    public ApiResponse<TopicSeedStatusResponse> getSeedTopicsStatus() {
        return ApiResponse.<TopicSeedStatusResponse>builder()
                .code(1000)
                .message("Get topic taxonomy seed status success")
                .result(topicSeedService.getSeedStatus())
                .build();
    }
}
