package com.publication_trend_tracking_system.sever_web_app.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {

    private Long paperId;

    private String reason;
}