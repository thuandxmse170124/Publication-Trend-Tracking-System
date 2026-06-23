package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTicketResponse {

    private Long reportId;

    private Long paperId;

    private String paperTitle;

    private String reason;

    private LocalDateTime createdAt;
}