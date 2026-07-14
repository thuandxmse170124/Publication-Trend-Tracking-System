package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateReportRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ReportTicketResponse;

import java.util.List;

public interface ReportService {

    void createReport(
            CreateReportRequest request,
            String email);

    List<ReportTicketResponse>
    getMyReports(
            String email);

    List<ReportTicketResponse>
    getAllReports();
}