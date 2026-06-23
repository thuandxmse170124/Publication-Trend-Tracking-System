package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.ReportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportTicketRepository
        extends JpaRepository<ReportTicket, Long> {

    List<ReportTicket>
    findByUserUserId(
            Long userId);
}