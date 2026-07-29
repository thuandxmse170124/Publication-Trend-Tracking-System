package com.publication_trend_tracking_system.sever_web_app.runner;

import com.publication_trend_tracking_system.sever_web_app.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppStartupRunner implements CommandLineRunner {

    private final SyncJobRepository syncJobRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking for any zombie sync jobs stuck in RUNNING state...");
        try {
            syncJobRepository.resetRunningJobsToFailed();
            log.info("Successfully cleaned up zombie sync jobs.");
        } catch (Exception e) {
            log.error("Failed to clean up zombie sync jobs: {}", e.getMessage());
        }
    }
}
