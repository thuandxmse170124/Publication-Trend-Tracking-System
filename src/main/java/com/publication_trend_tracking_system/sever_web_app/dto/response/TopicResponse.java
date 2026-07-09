package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicResponse {
    private Integer topicId;
    private String topicName;
    private String description;
    private Long paperCount;
    private List<PaperResponse> latestPapers;
}
