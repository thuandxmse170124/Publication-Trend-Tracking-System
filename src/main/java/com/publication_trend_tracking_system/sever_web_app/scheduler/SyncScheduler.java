package com.publication_trend_tracking_system.sever_web_app.scheduler;

import com.publication_trend_tracking_system.sever_web_app.entity.ApiSource;
import com.publication_trend_tracking_system.sever_web_app.repository.ApiSourceRepository;
import com.publication_trend_tracking_system.sever_web_app.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final SyncService syncService;
    private final ApiSourceRepository apiSourceRepository;

    @Scheduled(cron = "${app.sync.cron:0 0 2 * * ?}")
    public void runScheduledSync() {
        log.info("Starting scheduled background synchronization task...");
        List<ApiSource> activeSources = apiSourceRepository.findAll();
        for (ApiSource source : activeSources) {
            if ("ACTIVE".equalsIgnoreCase(source.getStatus())) {
                try {
                    log.info("Triggering scheduled sync for source: {}", source.getSourceName());
                    syncService.syncFromSource(source.getSourceId(), null, null);
                } catch (Exception ex) {
                    log.error("Failed to run scheduled sync for source: " + source.getSourceName(), ex);
                }
            }
        }
        log.info("Scheduled background synchronization task completed.");
    }
}
