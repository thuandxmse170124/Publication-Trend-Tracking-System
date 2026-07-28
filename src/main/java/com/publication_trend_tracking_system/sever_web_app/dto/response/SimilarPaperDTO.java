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
public class SimilarPaperDTO {
    private String paperId;
    private String title;
    private Integer publicationYear;
    private Integer citationCount;
    private List<String> authors;
    private String sourceUrl;
    private String doi;
    private Double similarityScore;
    private String similarityPercentage;
}
