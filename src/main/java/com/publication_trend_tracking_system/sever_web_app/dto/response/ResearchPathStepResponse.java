package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchPathStepResponse {
    private Integer order;
    private String purpose;
    private String reason;
    private CitationPaperNodeResponse paper;
}
