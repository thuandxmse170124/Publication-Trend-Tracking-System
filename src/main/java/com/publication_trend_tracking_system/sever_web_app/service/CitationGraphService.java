package com.publication_trend_tracking_system.sever_web_app.service;


import com.publication_trend_tracking_system.sever_web_app.dto.response.*;

import java.util.List;

public interface CitationGraphService {

    CitationGraphResponse getCitationGraph(Long paperId);

    CitationPaperPreviewResponse getPaperPreview(
            String openAlexId
    );
    CitationAuthorPreviewResponse getAuthorPreview(
            String authorId
    );
    List<CitationPaperNodeResponse> getReferences(Long paperId);

    List<CitationPaperNodeResponse> getCitedBy(
            Long paperId
    );
    CitationRelationshipResponse getRelationship(
            Long paperId,
            String referenceOpenAlexId
    );

    /** Primary node-click API for the Research Context tab. */
    ResearchContextResponse getResearchContext(Long paperId);

    /** Research Context for a graph node that is identified by OpenAlex. */
    ResearchContextResponse getResearchContextByOpenAlexId(String openAlexId);

    /**
     * Kept temporarily for clients that already use the former endpoint name.
     * New clients should call getResearchContext instead.
     */
    ResearchGuideResponse getResearchGuide(Long paperId);

}
