package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.ResearchField;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchFieldRepository extends JpaRepository<ResearchField, Integer> {
    java.util.Optional<ResearchField> findFirstByFieldNameIgnoreCase(String fieldName);
}
