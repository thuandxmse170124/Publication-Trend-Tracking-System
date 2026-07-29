package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationPaperPreviewResponse {

    private String openAlexId;

    private String title;

    private String paperAbstract;

    private Integer publicationYear;

    private Integer citedByCount;

    private String doi;

    private Boolean openAccess;

    private List<String> authors;

    private String primaryTopic;

    private List<String> topics;

    private Integer citationCount;

    private String journal;
}