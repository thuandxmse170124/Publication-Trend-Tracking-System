package com.publication_trend_tracking_system.sever_web_app.dto.response;

import com.publication_trend_tracking_system.sever_web_app.entity.ReportTicket;
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

    private ReportTicket.ReportStatus status;

    private String adminResponse;

    private String reporterEmail;
}