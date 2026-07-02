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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sync")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class AdminSyncController {

    private final SyncService syncService;
    private final UserRepository userRepository;

    @PostMapping("/trigger/{sourceId}")
    public ApiResponse<SyncJobResponse> triggerSync(
            @PathVariable Integer sourceId,
            @RequestParam(required = false) String query,
            Authentication authentication) {

        User user = null;
        if (authentication != null) {
            String email = authentication.getName();
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        Long userId = user != null ? user.getUserId() : null;
        SyncJobResponse result = syncService.syncFromSource(sourceId, userId, query);

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
            Authentication authentication) {

        User user = null;
        if (authentication != null) {
            user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        Long userId = user != null ? user.getUserId() : null;
        SyncJobResponse result = syncService.syncAll(sourceId, userId);

        return ApiResponse.<SyncJobResponse>builder()
                .code(1000)
                .message("Sync-all job started in background")
                .result(result)
                .build();
    }
}
