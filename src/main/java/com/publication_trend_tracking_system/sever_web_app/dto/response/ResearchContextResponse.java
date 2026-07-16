package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The researcher-facing context for a selected paper node.
 *
 * <p>This is intentionally named "Research Context" rather than "Research
 * Guide": the response is derived from citation and bibliographic metadata,
 * not an AI recommendation.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchContextResponse {
    private String researchArea;
    private List<CitationPaperNodeResponse> keyReferences;
    private List<ResearchPathStepResponse> readingPath;
    private List<CitationPaperNodeResponse> influencedStudies;
    private List<CitationReasonResponse> citationInsights;
}
