package com.publication_trend_tracking_system.sever_web_app.dto.request;

import com.publication_trend_tracking_system.sever_web_app.enums.PaperPublicationType;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperRequest {

    private Integer journalId;

    private Integer fieldId;

    private Integer apiSourceId;

    @NotNull
    private PaperPublicationType publicationType;

    @NotBlank
    private String title;

    private String paperAbstract;

    @PositiveOrZero
    private Integer publicationYear;

    private String doi;

    private String sourceUrl;

    @NotNull
    @PositiveOrZero
    private Integer citationCount;

    @NotNull
    private PaperVisibilityStatus visibilityStatus;
}
