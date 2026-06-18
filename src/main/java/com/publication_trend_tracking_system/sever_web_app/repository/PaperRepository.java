package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository
        extends JpaRepository<Paper, Long> {
}