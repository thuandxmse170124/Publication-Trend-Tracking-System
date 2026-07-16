package com.publication_trend_tracking_system.sever_web_app.dto.response;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationPaperNodeResponse {

    private String openAlexId;

    private String title;

    private Integer publicationYear;

    private Integer citedByCount;

    private String doi;
}