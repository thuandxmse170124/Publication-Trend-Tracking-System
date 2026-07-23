package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.TopicField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicFieldRepository extends JpaRepository<TopicField, Integer> {
    Optional<TopicField> findByOpenalexId(String openalexId);
}
