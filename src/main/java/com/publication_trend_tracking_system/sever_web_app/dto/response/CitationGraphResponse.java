package com.publication_trend_tracking_system.sever_web_app.dto.response;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationGraphResponse {

    private CitationCenterPaperResponse centerPaper;

    private Integer totalReferences;

    private List<CitationAuthorGroupResponse> authorGroups;
}