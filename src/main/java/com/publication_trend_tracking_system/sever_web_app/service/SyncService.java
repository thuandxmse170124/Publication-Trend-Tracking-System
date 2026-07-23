package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobPaperResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobResponse;
import org.springframework.data.domain.Page;

public interface SyncService {
    SyncJobResponse syncFromSource(Integer sourceId, Long userId, String customQuery, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange, String fromDate, String toDate);
    void executeSyncJob(Long jobId, Integer sourceId, String customQuery, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange, String fromDate, String toDate);
    void executeScheduledSyncJob(Long jobId, Integer sourceId);
    Page<SyncJobResponse> getSyncLogs(int page, int size);
    Page<SyncJobPaperResponse> getSyncJobPapers(Long jobId, int page, int size);
    SyncJobResponse retrySyncJob(Long jobId, Long userId);
    SyncJobResponse syncAll(Integer sourceId, Long userId, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange timeRange, String fromDate, String toDate);
    void stopSyncJob(Long jobId);

    // Scheduled background sync (Implementation Plan v3 optimization): only re-checks topics
    // that already have papers, instead of sweeping the full taxonomy on every scheduled run.
    // Admin-triggered "Sync All" keeps doing the full sweep via syncAll() above.
    void triggerScheduledSync(Integer sourceId);

    // Global on/off switch for the weekly background sync scheduler (admin UI toggle).
    boolean isSchedulerEnabled();
    boolean setSchedulerEnabled(boolean enabled);
}
