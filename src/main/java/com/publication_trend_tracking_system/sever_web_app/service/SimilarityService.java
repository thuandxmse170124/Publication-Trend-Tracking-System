package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.SimilarityResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface SimilarityService {
    SimilarityResponseDTO analyzeSimilarity(MultipartFile file);
}
