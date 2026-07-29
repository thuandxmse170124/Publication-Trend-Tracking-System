package com.publication_trend_tracking_system.sever_web_app.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

// In-memory run/cancel state for the topic taxonomy seed job. The seed is a single global
// @Async operation (not per-source like sync jobs), so unlike SyncJob there's no DB row to track
// it — a backend restart already kills the async thread and naturally resets this state, which is
// the correct behavior since nothing is running anymore after restart.
@Component
public class TopicSeedState {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    /** Returns false if a seed is already running (caller should not start another). */
    public boolean tryStart() {
        return running.compareAndSet(false, true);
    }

    public void finish() {
        running.set(false);
        cancelRequested.set(false);
    }

    public void requestCancel() {
        cancelRequested.set(true);
    }

    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    public boolean isRunning() {
        return running.get();
    }
}
