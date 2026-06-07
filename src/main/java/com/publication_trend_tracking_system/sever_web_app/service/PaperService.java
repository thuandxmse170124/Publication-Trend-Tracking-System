package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;

import java.util.List;

public interface PaperService {

    PaperResponse createPaper(PaperRequest request);

    List<PaperResponse> getAllPapers(String keyword);

    PaperResponse getPaperById(Long paperId);

    PaperResponse updatePaper(Long paperId, PaperRequest request);

    void deletePaper(Long paperId);
}
