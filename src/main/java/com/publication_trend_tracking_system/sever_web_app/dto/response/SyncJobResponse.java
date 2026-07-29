package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJobResponse {

    private Long syncJobId;

    private Integer sourceId;

    private String sourceName;

    private String triggeredByEmail;

    private String status;

    private Integer addedCount;

    private Integer updatedCount;

    private String errorMessage;

    // Null for custom-query / Semantic Scholar jobs; set only for structured OpenAlex "Sync All"
    // runs that iterate the official topic taxonomy (Implementation Plan v3).
    private Integer totalTopicsCount;

    private Integer processedTopicsCount;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
