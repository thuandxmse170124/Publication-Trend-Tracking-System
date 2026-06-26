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

    @Query(value = "SELECT TOP 5 t.* FROM topics t " +
                   "LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id " +
                   "GROUP BY t.topic_id, t.topic_name, t.description " +
                   "ORDER BY COUNT(pt.paper_id) DESC", nativeQuery = true)
    java.util.List<Topic> findTop5TrendingTopics();

    @Query(value = "SELECT t.topic_id, t.topic_name, COUNT(pt.paper_id) FROM topics t LEFT JOIN paper_topics pt ON t.topic_id = pt.topic_id GROUP BY t.topic_id, t.topic_name ORDER BY COUNT(pt.paper_id) DESC", nativeQuery = true)
    java.util.List<Object[]> findAllTopicsWithCount();
}
