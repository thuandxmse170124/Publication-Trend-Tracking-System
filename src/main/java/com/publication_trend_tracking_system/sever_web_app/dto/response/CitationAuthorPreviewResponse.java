package com.publication_trend_tracking_system.sever_web_app.dto.response;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationAuthorPreviewResponse {

    private String openAlexAuthorId;

    private String displayName;

    private String orcid;

    private String institutionName;

    private String institutionCountryCode;

    private Integer worksCount;

    private Integer citedByCount;

    private List<String> researchTopics;
}