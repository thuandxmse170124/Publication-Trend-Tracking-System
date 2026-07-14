package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SearchHistoryResponse {
    private Long historyId;
    private String keyword;
    private LocalDateTime searchedAt;
}
