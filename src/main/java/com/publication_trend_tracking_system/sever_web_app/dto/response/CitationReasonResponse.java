package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationReasonResponse {
    private CitationPaperNodeResponse citingPaper;
    private String relationship;
    private String connectionStrength;
    private String reason;
    private CitationInsightEvidenceResponse evidence;
}
