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

    // The zone is pinned rather than inherited from the host: without it the same build fires at a
    // different local hour on a machine configured for another timezone, which is a confusing way
    // to discover that a nightly job ran in the middle of the working day.
    // The look-back window is app.sync.scheduled-time-range — keep it at least as wide as the gap
    // between two firings of this cron, or the run misses papers published in between.
    @Scheduled(cron = "${app.sync.cron:0 0 2 * * SUN}", zone = "${app.sync.cron-zone:Asia/Ho_Chi_Minh}")
    public void runScheduledSync() {
        if (!syncService.isSchedulerEnabled()) {
            log.info("Scheduled sync skipped: background sync is disabled via admin toggle.");
            return;
        }
        log.info("Submitting scheduled background synchronization jobs...");
        int submitted = 0;
        List<ApiSource> activeSources = apiSourceRepository.findAll();
        for (ApiSource source : activeSources) {
            if ("ACTIVE".equalsIgnoreCase(source.getStatus())) {
                try {
                    log.info("Triggering scheduled sync for source: {}", source.getSourceName());
                    syncService.triggerScheduledSync(source.getSourceId());
                    submitted++;
                } catch (Exception ex) {
                    log.error("Failed to run scheduled sync for source: " + source.getSourceName(), ex);
                }
            }
        }
        // Each job runs asynchronously, so this marks the end of submission, not of the sync work.
        log.info("Submitted {} scheduled sync job(s); they continue in the background.", submitted);
    }

    // Safety net for a job whose thread died without writing a final status. Its row would sit at
    // RUNNING forever, and since a RUNNING job blocks new syncs for the same source, that single
    // row would lock the source out until someone restarted the server.
    @Scheduled(fixedDelayString = "${app.sync.stale-job-check-ms:300000}",
            initialDelayString = "${app.sync.stale-job-check-ms:300000}")
    public void releaseStalledJobs() {
        try {
            syncService.failStaleRunningJobs();
        } catch (Exception ex) {
            log.error("Stalled-sync-job sweep failed", ex);
        }
    }
}
