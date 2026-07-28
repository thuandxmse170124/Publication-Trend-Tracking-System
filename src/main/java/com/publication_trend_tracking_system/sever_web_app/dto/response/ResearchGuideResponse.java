package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchGuideResponse {
    private String researchArea;
    private List<CitationPaperNodeResponse> keyReferences;
    private List<ResearchPathStepResponse> readingPath;
    private List<CitationPaperNodeResponse> influencedStudies;
    private List<CitationReasonResponse> citationInsights;
}
