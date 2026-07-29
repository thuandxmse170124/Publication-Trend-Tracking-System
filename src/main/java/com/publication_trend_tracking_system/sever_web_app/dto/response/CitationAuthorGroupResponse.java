package com.publication_trend_tracking_system.sever_web_app.dto.response;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationAuthorGroupResponse {

    private String authorId;

    private String authorName;

    private Integer paperCount;

    private List<CitationPaperNodeResponse> papers;
}