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

    ResearchGuideResponse getResearchGuide(Long paperId);

}
