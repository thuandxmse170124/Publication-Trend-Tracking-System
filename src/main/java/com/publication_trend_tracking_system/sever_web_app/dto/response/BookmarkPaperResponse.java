package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkPaperResponse {

    private Long bookmarkId;

    private Long paperId;

    private String title;

    private String note;

    private LocalDateTime savedAt;
}