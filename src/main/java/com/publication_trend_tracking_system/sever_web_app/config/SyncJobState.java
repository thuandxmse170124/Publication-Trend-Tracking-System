package com.publication_trend_tracking_system.sever_web_app.config;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cancel signals for running sync jobs.
 *
 * <p>Worker threads previously re-read the SyncJob row from the database before every page of every
 * query to see whether an admin had pressed Stop. With five worker threads per job that is a
 * constant stream of extra connection checkouts competing with the batch saves doing the actual
 * work, and it made cancellation cost a query per page even in the normal case where nobody
 * cancelled anything.
 *
 * <p>The database row remains the durable record of a job's status — this only carries the live
 * signal. A backend restart drops these flags, which is correct: nothing is running after a restart,
 * and jobs left stranded in RUNNING are reset to FAILED on startup.
 */
@Component
public class SyncJobState {

    private final Set<Long> canceledJobIds = ConcurrentHashMap.newKeySet();

    public void requestCancel(Long jobId) {
        canceledJobIds.add(jobId);
    }

    public boolean isCancelRequested(Long jobId) {
        return canceledJobIds.contains(jobId);
    }

    /** Called when a job reaches a terminal state, so the set doesn't grow without bound. */
    public void clear(Long jobId) {
        canceledJobIds.remove(jobId);
    }
}
