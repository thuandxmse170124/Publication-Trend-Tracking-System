package com.publication_trend_tracking_system.sever_web_app.service;


import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationAuthorPreviewResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationGraphResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationPaperPreviewResponse;

public interface CitationGraphService {

    CitationGraphResponse getCitationGraph(Long paperId);

    CitationPaperPreviewResponse getPaperPreview(
            String openAlexId
    );
    CitationAuthorPreviewResponse getAuthorPreview(
            String authorId
    );
}