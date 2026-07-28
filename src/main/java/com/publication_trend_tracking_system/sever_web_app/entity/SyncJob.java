package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_job_id")
    private Long syncJobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private ApiSource apiSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    @Column(name = "status", nullable = false)
    private String status;

    @Builder.Default
    @Column(name = "added_count", nullable = false)
    private Integer addedCount = 0;

    @Builder.Default
    @Column(name = "updated_count", nullable = false)
    private Integer updatedCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    // Populated only for structured OpenAlex "Sync All" runs (Implementation Plan v3), so the
    // frontend can show "processed / total official topics" progress. Null for custom-query and
    // Semantic Scholar jobs, which don't iterate the official topic taxonomy.
    @Column(name = "total_topics_count")
    private Integer totalTopicsCount;

    @Builder.Default
    @Column(name = "processed_topics_count")
    private Integer processedTopicsCount = 0;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
