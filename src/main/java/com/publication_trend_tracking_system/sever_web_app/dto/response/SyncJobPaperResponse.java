package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJobPaperResponse {

    private Long paperId;

    private String title;

    // "ADDED" or "UPDATED" — reflects this paper's most recent sync touch.
    private String action;

    private String journalName;

    private Integer publicationYear;

    private String doi;

    private LocalDateTime updatedAt;
}
