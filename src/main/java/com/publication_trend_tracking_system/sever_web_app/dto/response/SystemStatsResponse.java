package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemStatsResponse {
    long totalPapers;
    List<TopicResponse> topTopics;
    String publicationTrend;
}
