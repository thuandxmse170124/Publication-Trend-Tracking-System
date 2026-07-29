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

    // A job whose thread died without finalising its row stays RUNNING forever, and because a
    // RUNNING job blocks every new sync for the same source, one dead job locks that source out
    // until someone restarts the server. The watchdog sweep uses this to find them.
    @org.springframework.data.jpa.repository.Query(
            "SELECT s FROM SyncJob s WHERE s.status = 'RUNNING' AND s.startedAt < :threshold")
    java.util.List<SyncJob> findStaleRunningJobs(
            @org.springframework.data.repository.query.Param("threshold") java.time.LocalDateTime threshold);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SyncJob s SET s.status = 'FAILED', s.errorMessage = 'Server restarted abruptly', s.finishedAt = CURRENT_TIMESTAMP WHERE s.status = 'RUNNING'")
    void resetRunningJobsToFailed();

    // Atomic increment (not read-modify-write) since many concurrent topic futures call this
    // for the same job. @Transactional here because callers invoke it standalone, with no
    // surrounding transaction of their own (unlike resetRunningJobsToFailed, whose caller is
    // already @Transactional) — without it, @Modifying throws TransactionRequiredException.
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SyncJob s SET s.processedTopicsCount = s.processedTopicsCount + 1 WHERE s.syncJobId = :jobId")
    void incrementProcessedTopicsCount(@org.springframework.data.repository.query.Param("jobId") Long jobId);

    // A batched request covers several topics, so progress advances by more than one at a time.
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "UPDATE SyncJob s SET s.processedTopicsCount = s.processedTopicsCount + :delta WHERE s.syncJobId = :jobId")
    void addProcessedTopicsCount(
            @org.springframework.data.repository.query.Param("jobId") Long jobId,
            @org.springframework.data.repository.query.Param("delta") int delta);
}
