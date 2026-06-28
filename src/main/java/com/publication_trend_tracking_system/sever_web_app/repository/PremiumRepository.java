package com.publication_trend_tracking_system.sever_web_app.repository;


import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumRepository
        extends JpaRepository<Premium, Long> {
}