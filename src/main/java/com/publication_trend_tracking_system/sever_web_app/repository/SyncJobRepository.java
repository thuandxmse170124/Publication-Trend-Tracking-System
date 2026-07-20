package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.SyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {
    Page<SyncJob> findAllByOrderByStartedAtDesc(Pageable pageable);

    boolean existsByApiSource_SourceIdAndStatus(Integer sourceId, String status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SyncJob s SET s.status = 'FAILED', s.errorMessage = 'Server restarted abruptly', s.finishedAt = CURRENT_TIMESTAMP WHERE s.status = 'RUNNING'")
    void resetRunningJobsToFailed();
}
