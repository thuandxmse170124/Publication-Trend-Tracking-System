package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.SyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {
    Page<SyncJob> findAllByOrderByStartedAtDesc(Pageable pageable);
}
