package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopJournalResponse {
    private String journalName;
    private Long paperCount;
}
