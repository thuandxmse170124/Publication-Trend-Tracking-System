package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.SyncJobResponse;
import org.springframework.data.domain.Page;

public interface SyncService {
    SyncJobResponse syncFromSource(Integer sourceId, Long userId, String customQuery);
    Page<SyncJobResponse> getSyncLogs(int page, int size);
    SyncJobResponse retrySyncJob(Long jobId, Long userId);
}
