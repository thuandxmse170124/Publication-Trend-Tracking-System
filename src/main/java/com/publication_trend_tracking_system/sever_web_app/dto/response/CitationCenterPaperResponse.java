package com.publication_trend_tracking_system.sever_web_app.dto.response;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationCenterPaperResponse {

    private Long paperId;

    private String openAlexId;

    private String title;

    private Integer publicationYear;

    private Integer citationCount;
}