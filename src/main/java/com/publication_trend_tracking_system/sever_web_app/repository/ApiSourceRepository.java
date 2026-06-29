package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.ApiSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiSourceRepository extends JpaRepository<ApiSource, Integer> {
    java.util.Optional<ApiSource> findByStatusIgnoreCase(String status);
}
