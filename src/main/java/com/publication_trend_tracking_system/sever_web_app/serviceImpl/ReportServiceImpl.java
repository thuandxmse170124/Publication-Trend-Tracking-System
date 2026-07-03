package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateReportRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ReportTicketResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.entity.ReportTicket;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.ReportTicketRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl
        implements ReportService {

    private final ReportTicketRepository
            reportTicketRepository;

    private final PaperRepository
            paperRepository;

    private final UserRepository
            userRepository;

    @Override
    public void createReport(
            CreateReportRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        Paper paper =
                paperRepository
                        .findById(
                                request.getPaperId())
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.PAPER_NOT_FOUND));

        ReportTicket reportTicket =
                ReportTicket.builder()
                        .paper(paper)
                        .user(user)
                        .reason(
                                request.getReason())
                        .build();

        reportTicketRepository.save(
                reportTicket);
    }

    @Override
    public List<ReportTicketResponse>
    getMyReports(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return reportTicketRepository
                .findByUserUserId(
                        user.getUserId())
                .stream()
                .map(report ->
                        ReportTicketResponse.builder()
                                .reportId(
                                        report.getReportId())
                                .paperId(
                                        report.getPaper()
                                                .getPaperId())
                                .paperTitle(
                                        report.getPaper()
                                                .getTitle())
                                .reason(
                                        report.getReason())
                                .createdAt(
                                        report.getCreatedAt())
                                .build())
                .toList();
    }

    @Override
    public List<ReportTicketResponse>
    getAllReports() {

        return reportTicketRepository
                .findAll()
                .stream()
                .map(report ->
                        ReportTicketResponse.builder()
                                .reportId(
                                        report.getReportId())
                                .paperId(
                                        report.getPaper()
                                                .getPaperId())
                                .paperTitle(
                                        report.getPaper()
                                                .getTitle())
                                .reason(
                                        report.getReason())
                                .createdAt(
                                        report.getCreatedAt())
                                .build())
                .toList();
    }
}