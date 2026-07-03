package com.publication_trend_tracking_system.sever_web_app.dto.request;

import com.publication_trend_tracking_system.sever_web_app.entity.ReportTicket;
import lombok.*;

@Getter
@Setter
public class UpdateReportRequest {

    private String adminResponse;

    private ReportTicket.ReportStatus status;
}