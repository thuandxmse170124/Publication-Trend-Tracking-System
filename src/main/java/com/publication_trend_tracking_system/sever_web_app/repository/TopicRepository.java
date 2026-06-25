package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {
    Optional<Topic> findByTopicNameIgnoreCase(String topicName);

    @Query("SELECT COUNT(p) FROM Paper p JOIN p.topics t WHERE t.topicId = :topicId")
    long countPapersByTopicId(@Param("topicId") Integer topicId);
}
