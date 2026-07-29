package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityResponseDTO {
    private String uploadedFileName;
    private List<String> extractedKeywords;
    private List<SimilarPaperDTO> similarPapers;
    // Count of the papers actually returned in similarPapers (post top-5 truncation), so the FE
    // "N Similar Papers" badge matches what's rendered below it instead of showing "undefined".
    private Integer totalMatches;
}
