package com.publication_trend_tracking_system.sever_web_app.dto.response;

import com.publication_trend_tracking_system.sever_web_app.enums.PaperPublicationType;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperResponse {

    private Long paperId;

    private Integer journalId;

    private String journalName;

    private Integer fieldId;

    private String fieldName;

    private Integer apiSourceId;

    private String apiSourceName;

    private PaperPublicationType publicationType;

    private String title;

    private String paperAbstract;

    private Integer publicationYear;

    private String doi;

    private String sourceUrl;

    private Integer citationCount;

    private PaperVisibilityStatus visibilityStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<AuthorResponse> authors;

    private List<String> keywords;

    private List<String> topics;
}
