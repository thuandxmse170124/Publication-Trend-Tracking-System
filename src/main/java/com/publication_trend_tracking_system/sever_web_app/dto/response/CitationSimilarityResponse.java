package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationSimilarityResponse {

    private Integer topicSimilarity;

    private Integer conceptSimilarity;

    private Integer keywordSimilarity;

    private Integer authorSimilarity;

    private Integer journalSimilarity;

    private Integer publicationYearSimilarity;

}