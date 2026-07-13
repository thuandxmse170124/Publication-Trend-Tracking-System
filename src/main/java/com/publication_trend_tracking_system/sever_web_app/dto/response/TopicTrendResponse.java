package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicTrendResponse {

    private Integer topicId;

    private String topicName;

    private Long paperCount;

    private Long previousPaperCount;

    private Long currentPaperCount;

    private Double growthRate;

    private String trend;
}