package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchFieldResponse {
    private Integer fieldId;
    private String fieldName;
    private String description;
}
